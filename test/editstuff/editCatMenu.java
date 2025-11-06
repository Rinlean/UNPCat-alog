package editstuff;



import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import main.stuff.dbconn;

/**
 * EditCatMenu retrieves cats linked to the given caretaker/account id directly
 * from the DB using its own dbconn. When a cat is selected it will notify an
 * optional catProfileMenu instance by calling setCatId(...) so the existing
 * profile window repaints instead of opening a new one.
 */
public class editCatMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(editCatMenu.class.getName());

    private final int accountId;
    private final catProfileMenu profileWindow; // may be null in standalone usage

    private final DefaultComboBoxModel<CatItem> comboModel = new DefaultComboBoxModel<>();
    private final JComboBox<CatItem> comboCats = new JComboBox<>(comboModel);
    private final JButton btnRefresh = new JButton("Refresh");
    private final JLabel lblTitle = new JLabel("Select a cat:");

    public editCatMenu(int accountId) {
        this(accountId, null);
    }

    /**
     * @param accountId caretaker/account id used to find linked cats
     * @param profileWindow optional reference to a catProfileMenu instance to
     * update when selection changes
     */
    public editCatMenu(int accountId, catProfileMenu profileWindow) {
        this.accountId = accountId;
        this.profileWindow = profileWindow;
        initComponents();
        loadCats(); // load list from DB using this class's own dbconn
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(400, 120));
        setMinimumSize(new java.awt.Dimension(400, 120));
        setResizable(false);
        setTitle("Edit Cat (Account " + accountId + ")");
        getContentPane().setLayout(null);

        lblTitle.setBounds(10, 10, 120, 25);
        getContentPane().add(lblTitle);

        comboCats.setBounds(10, 40, 260, 25);
        comboCats.setFocusable(true);
        getContentPane().add(comboCats);

        btnRefresh.setBounds(280, 40, 100, 25);
        getContentPane().add(btnRefresh);

        comboCats.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            CatItem sel = (CatItem) comboCats.getSelectedItem();
            if (sel == null || sel.id <= 0) {
                return;
            }
            if (profileWindow != null) {
                // reuse existing profile window and trigger repaint/refresh
                SwingUtilities.invokeLater(() -> {
                    profileWindow.setCatId(sel.id);   // triggers background fetch & UI update
                    profileWindow.setVisible(true);
                    profileWindow.toFront();
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    catProfileMenu win = new catProfileMenu(sel.id);
                    win.setVisible(true);
                });
            }
        });

        btnRefresh.addActionListener(evt -> loadCats());

        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        pack();
        setLocationRelativeTo(null);
    }

    private Connection getConnection() throws Exception {
        return dbconn.getConnection();
    }

    private void loadCats() {
        // load cat list in background to avoid blocking UI; uses this class's own dbconn
        new SwingWorker<List<CatItem>, Void>() {
            Exception error;

            @Override
            protected List<CatItem> doInBackground() {
                List<CatItem> items = new ArrayList<>();
                String sql = "SELECT c.cat_id, c.name FROM cat_caretaker cc JOIN cat c ON cc.cat_id = c.cat_id WHERE cc.caretaker_id = ? ORDER BY c.name";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("cat_id");
                            String name = rs.getString("name");
                            items.add(new CatItem(id, name));
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                }
                return items;
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(editCatMenu.this,
                            "Error loading cats:\n" + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    error.printStackTrace();
                    return;
                }
                try {
                    List<CatItem> items = get();
                    comboModel.removeAllElements();
                    comboModel.addElement(new CatItem(-1, "-- Select a cat --"));
                    for (CatItem it : items) {
                        comboModel.addElement(it);
                    }
                    // if profileWindow already shows a cat, try to pre-select it
                    if (profileWindow != null) {
                        int current = profileWindow.getCurrentCatId();
                        if (current > 0) {
                            for (int i = 0; i < comboModel.getSize(); i++) {
                                CatItem ci = comboModel.getElementAt(i);
                                if (ci != null && ci.id == current) {
                                    comboCats.setSelectedIndex(i);
                                    return;
                                }
                            }
                        }
                    }
                    comboCats.setSelectedIndex(0);
                } catch (Exception ex) {
                    logger.severe("Unexpected error updating combo: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private static final class CatItem {

        final int id;
        final String name;

        CatItem(int id, String name) {
            this.id = id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name.isEmpty() ? ("#" + id) : name + " (#" + id + ")";
        }
    }

    /**
     * Quick standalone launcher for development/testing.
     */
    public static void main(String args[]) {
        int acct = 1;
        if (args.length > 0) {
            try {
                acct = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        final int accountId = acct;
        java.awt.EventQueue.invokeLater(() -> new editCatMenu(accountId).setVisible(true));
    }

    // Variables declaration - do not modify
    // End of variables declaration                   
}

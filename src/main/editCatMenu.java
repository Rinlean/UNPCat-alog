package main;

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
 * Simple editor panel that lists cats linked to a caretaker/account id. When a
 * cat is selected the provided catProfileMenu (if any) will be updated via
 * setCatId(...)
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
        loadCats();
    }

    private Connection getConnection() throws Exception {
        return dbconn.getConnection();
    }

    private void loadCats() {
        // load cat list in background to avoid blocking UI
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
                    // preserve selection: if profileWindow has a valid cat id, select it
                    if (profileWindow != null) {
                        int current = profileWindow == null ? -1 : profileWindow.lblId.getText().isEmpty() ? -1 : parseIntSafe(profileWindow.lblId.getText());
                        if (current > 0) {
                            for (int i = 0; i < comboModel.getSize(); i++) {
                                CatItem ci = comboModel.getElementAt(i);
                                if (ci != null && ci.id == current) {
                                    comboCats.setSelectedIndex(i);
                                    break;
                                }
                            }
                        } else {
                            comboCats.setSelectedIndex(0);
                        }
                    } else {
                        comboCats.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    logger.severe("Unexpected error updating combo: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
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

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(180, 720));
        setMinimumSize(new java.awt.Dimension(180, 720));
        setResizable(false);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(55, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(114, 114, 114)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(584, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new editCatMenu(1).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBox1;
    // End of variables declaration//GEN-END:variables
}

package main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import main.stuff.dbconn;

public class catProfileMenu extends javax.swing.JFrame {

    private int catId;
    private final DefaultTableModel healthModel = new DefaultTableModel(new Object[]{"Date", "Conditions"}, 0);
    private final DefaultTableModel caretakersModel = new DefaultTableModel(new Object[]{"Name", "Contact"}, 0);
    private final DefaultTableModel incidentsModel = new DefaultTableModel(new Object[]{"Date", "Description"}, 0);

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(catProfileMenu.class.getName());

    private static final int AUTO_REFRESH_MS = 30_000;
    private final Timer autoRefreshTimer;

    public catProfileMenu(int catId) {
        this.catId = catId;
        initComponents();

        autoRefreshTimer = new Timer(AUTO_REFRESH_MS, e -> {
            if (this.catId > 0) {
                fetchAndPopulate();
            }
        });
        autoRefreshTimer.setRepeats(true);

        if (catId > 0) {
            fetchAndPopulate();
            autoRefreshTimer.start();
        } else {
            clearDisplay();
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (autoRefreshTimer.isRunning()) {
                    autoRefreshTimer.stop();
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (autoRefreshTimer.isRunning()) {
                    autoRefreshTimer.stop();
                }
            }
        });
    }


    public void setCatId(int newCatId) {
        if (this.catId == newCatId) {
            return;
        }
        this.catId = newCatId;
        if (newCatId > 0) {
            fetchAndPopulate();
            if (!autoRefreshTimer.isRunning()) {
                autoRefreshTimer.start();
            }
        } else {
            clearDisplay();
            if (autoRefreshTimer.isRunning()) {
                autoRefreshTimer.stop();
            }
        }
    }

    public int getCurrentCatId() {
        return this.catId > 0 ? this.catId : -1;
    }

    public void refreshProfile() {
        if (this.catId > 0) {
            fetchAndPopulate();
        }
    }

    private void clearDisplay() {
        SwingUtilities.invokeLater(() -> {
            lblId.setText("");
            lblName.setText("");
            setTitle("Cat Profile");
            lblGender.setText("");
            lblBreed.setText("");
            lblColor.setText("");
            lblArea.setText("");
            lblAdoptionStatus.setText("");
            lblAdoptionWhen.setText("");
            lblAdoptionNotes.setText("");
            lblAdopter.setText("");
            taBehavior.setText("");
            healthModel.setRowCount(0);
            caretakersModel.setRowCount(0);
            incidentsModel.setRowCount(0);
        });
    }

    private Connection getConnection() throws Exception {
        return dbconn.getConnection();
    }

    private void fetchAndPopulate() {
        new SwingWorker<Void, Void>() {
            Exception error;

            @Override
            protected Void doInBackground() {
                try (Connection conn = getConnection()) {
                    populateBasic(conn);
                    populateAdoption(conn);
                    populateBehavior(conn);
                    populateHealth(conn);
                    populateCaretakers(conn);
                    populateIncidents(conn);
                } catch (Exception e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(catProfileMenu.this,
                            "Error loading cat data:\n" + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    error.printStackTrace();
                }
            }
        }.execute();
    }

    private void populateBasic(Connection conn) throws SQLException {
        String sql = "SELECT c.cat_id, c.name, c.gender, c.breed, c.color, a.area_name "
                + "FROM cat c LEFT JOIN area a ON c.area_id = a.area_id WHERE c.cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(catProfileMenu.this,
                                "No cat found with id " + catId, "Not found", JOptionPane.INFORMATION_MESSAGE);
                        clearDisplay();
                    });
                    return;
                }
                final int idVal = rs.getInt("cat_id");
                final String nameVal = rs.getString("name");
                final String genderVal = rs.getString("gender");
                final String breedVal = rs.getString("breed");
                final String colorVal = rs.getString("color");
                final String areaVal = rs.getString("area_name");

                SwingUtilities.invokeLater(() -> {
                    lblId.setText(String.valueOf(idVal));
                    lblName.setText(nvl(nameVal));
                    setTitle(nvl(nameVal) + "'s Profile");
                    lblGender.setText(nvl(genderVal));
                    lblBreed.setText(nvl(breedVal));
                    lblColor.setText(nvl(colorVal));
                    lblArea.setText(nvl(areaVal));
                });
            }
        }
    }

    private void populateAdoption(Connection conn) throws SQLException {
        String sql = "SELECT status, changed_at, notes, adopter_id FROM adoption_status WHERE cat_id = ? ORDER BY changed_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String status = rs.getString("status");
                    final Timestamp when = rs.getTimestamp("changed_at");
                    final String notes = rs.getString("notes");
                    final int adopterId = rs.getInt("adopter_id");
                    final boolean hasAdopter = !rs.wasNull();

                    SwingUtilities.invokeLater(() -> {
                        lblAdoptionStatus.setText(nvl(status));
                        lblAdoptionWhen.setText(when != null ? when.toString() : "");
                        lblAdoptionNotes.setText(nvl(notes));
                    });

                    if (hasAdopter) {
                        populateAdopter(conn, adopterId);
                    } else {
                        SwingUtilities.invokeLater(() -> lblAdopter.setText("n/a"));
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        lblAdoptionStatus.setText("No history");
                        lblAdoptionWhen.setText("");
                        lblAdoptionNotes.setText("");
                        lblAdopter.setText("");
                    });
                }
            }
        }
    }

    private void populateAdopter(Connection conn, int adopterId) {
        String sql = "SELECT name, contact_info FROM adopter WHERE adopter_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adopterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String name = rs.getString("name");
                    final String contact = rs.getString("contact_info");
                    SwingUtilities.invokeLater(() -> lblAdopter.setText(name + (contact != null ? " (" + contact + ")" : "")));
                } else {
                    SwingUtilities.invokeLater(() -> lblAdopter.setText("Adopter id " + adopterId + " not found"));
                }
            }
        } catch (SQLException e) {
            SwingUtilities.invokeLater(() -> lblAdopter.setText("Could not lookup adopter: " + e.getMessage()));
        }
    }

    private void populateBehavior(Connection conn) throws SQLException {
        String sql = "SELECT personality, notes FROM behavior WHERE cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    final String p = rs.getString("personality");
                    final String notes = rs.getString("notes");

                    if (p != null && !p.isEmpty()) {
                        sb.append("Behavior: ").append(p).append("\n");
                    }
                    if (notes != null && !notes.isEmpty()) {
                        sb.append("Notes: ").append(notes).append("\n");
                    }
                    sb.append("\n");
                }
                if (!found) {
                    sb.append("No behavior record.");
                }
                final String text = sb.toString().trim();
                SwingUtilities.invokeLater(() -> taBehavior.setText(text));
            }
        }
    }

    private void populateHealth(Connection conn) throws SQLException {
        tblHealth.getTableHeader().setReorderingAllowed(false);
        tblHealth.getTableHeader().setResizingAllowed(false);
        String sql = "SELECT conditions, `date` FROM health_record WHERE cat_id = ? ORDER BY `date` DESC LIMIT 20";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                SwingUtilities.invokeLater(() -> healthModel.setRowCount(0));
                while (rs.next()) {
                    final Date d = rs.getDate("date");
                    final String cond = rs.getString("conditions");
                    SwingUtilities.invokeLater(() -> healthModel.addRow(new Object[]{d, nvl(cond)}));
                }
            }
        } catch (SQLException e) {
            SwingUtilities.invokeLater(() -> {
                healthModel.setRowCount(0);
                healthModel.addRow(new Object[]{"Error", e.getMessage()});
            });
        }
    }

    private void populateCaretakers(Connection conn) throws SQLException {
        tblCaretakers.getTableHeader().setReorderingAllowed(false);
        tblCaretakers.getTableHeader().setResizingAllowed(false);
        String sql = "SELECT t.name, t.contact_info FROM caretaker t JOIN cat_caretaker cc ON t.caretaker_id = cc.caretaker_id WHERE cc.cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                SwingUtilities.invokeLater(() -> caretakersModel.setRowCount(0));
                while (rs.next()) {
                    final String name = rs.getString("name");
                    final String contact = rs.getString("contact_info");
                    SwingUtilities.invokeLater(() -> caretakersModel.addRow(new Object[]{nvl(name), nvl(contact)}));
                }
            }
        } catch (SQLException e) {
            SwingUtilities.invokeLater(() -> {
                caretakersModel.setRowCount(0);
                caretakersModel.addRow(new Object[]{"Error", e.getMessage()});
            });
        }
    }

    private void populateIncidents(Connection conn) throws SQLException {
        tblIncidents.getTableHeader().setReorderingAllowed(false);
        tblIncidents.getTableHeader().setResizingAllowed(false);
        String sql = "SELECT `date`, `desc` FROM incident_report WHERE cat_id = ? ORDER BY `date` DESC LIMIT 20";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                SwingUtilities.invokeLater(() -> incidentsModel.setRowCount(0));
                while (rs.next()) {
                    final Timestamp t = rs.getTimestamp("date");
                    final String d = rs.getString("desc");
                    SwingUtilities.invokeLater(() -> incidentsModel.addRow(new Object[]{t, nvl(d)}));
                }
            }
        } catch (SQLException e) {
            SwingUtilities.invokeLater(() -> {
                incidentsModel.setRowCount(0);
                incidentsModel.addRow(new Object[]{"Error", e.getMessage()});
            });
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        basicInfoPnl = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        adoptionPanel = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        healthScroll = new javax.swing.JScrollPane();
        incidentScroll = new javax.swing.JScrollPane();
        CaretakerScroll = new javax.swing.JScrollPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        taBehavior = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(720, 720));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        basicInfoPnl.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cat's Basic Information", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 1, 12))); // NOI18N
        basicInfoPnl.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Cat ID:");
        basicInfoPnl.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 30, -1, -1));

        jLabel2.setText("Name:");
        basicInfoPnl.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, -1, -1));

        jLabel3.setText("Gender:");
        basicInfoPnl.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, -1, -1));

        jLabel4.setText("Breed:");
        basicInfoPnl.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, -1, -1));

        jLabel5.setText("Color:");
        basicInfoPnl.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 150, -1, -1));

        jLabel6.setText("Area:");
        basicInfoPnl.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 180, -1, -1));
        basicInfoPnl.add(lblId, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 30, -1, -1));
        basicInfoPnl.add(lblName, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 60, -1, -1));
        basicInfoPnl.add(lblGender, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 90, -1, -1));
        basicInfoPnl.add(lblBreed, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 120, -1, -1));
        basicInfoPnl.add(lblColor, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 150, -1, -1));
        basicInfoPnl.add(lblArea, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 180, -1, -1));

        getContentPane().add(basicInfoPnl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 220));

        adoptionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Adoption Status", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 1, 12))); // NOI18N
        adoptionPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel13.setText("Status:");
        adoptionPanel.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 40, -1, -1));

        jLabel14.setText("When:");
        adoptionPanel.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, -1, -1));

        jLabel15.setText("Notes:");
        adoptionPanel.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, -1, -1));

        jLabel16.setText("Adopter:");
        adoptionPanel.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, -1, -1));
        adoptionPanel.add(lblAdoptionStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 40, -1, -1));
        adoptionPanel.add(lblAdoptionWhen, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 70, -1, -1));
        adoptionPanel.add(lblAdoptionNotes, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 100, -1, -1));
        adoptionPanel.add(lblAdopter, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 130, -1, -1));

        getContentPane().add(adoptionPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 0, 320, 220));

        tblHealth.setModel(healthModel);
        tblHealth.setRowSelectionAllowed(false);
        healthScroll.setViewportView(tblHealth);

        jTabbedPane1.addTab("Health", healthScroll);

        tblIncidents.setModel(incidentsModel);
        tblIncidents.setRowSelectionAllowed(false);
        incidentScroll.setViewportView(tblIncidents);

        jTabbedPane1.addTab("Incidents", incidentScroll);

        tblCaretakers.setModel(caretakersModel);
        tblCaretakers.setRowSelectionAllowed(false);
        CaretakerScroll.setViewportView(tblCaretakers);

        jTabbedPane1.addTab("Caretaker", CaretakerScroll);

        getContentPane().add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 360, 720, 360));

        jScrollPane4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Behavior and Notes", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 1, 12))); // NOI18N
        jScrollPane4.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        taBehavior.setEditable(false);
        taBehavior.setColumns(20);
        taBehavior.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        taBehavior.setLineWrap(true);
        taBehavior.setRows(5);
        taBehavior.setWrapStyleWord(true);
        taBehavior.setFocusable(false);
        jScrollPane4.setViewportView(taBehavior);

        getContentPane().add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 220, 720, 140));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public static void testMenu() {
        int id = -1;
        String input = JOptionPane.showInputDialog(null, "Enter cat_id to view:", "Open Cat Profile", JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return; // user cancelled
        }
        try {
            id = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid cat_id: " + input, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int catId = id;
        SwingUtilities.invokeLater(() -> {
            catProfileMenu win = new catProfileMenu(catId);
            win.setVisible(true);
        });
    }

    public static void testMenu(String[] args) {
        if (args != null && args.length > 0) {
            int id;
            try {
                id = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid cat_id argument: " + args[0], "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            final int catId = id;
            SwingUtilities.invokeLater(() -> {
                catProfileMenu win = new catProfileMenu(catId);
                win.setVisible(true);
            });
        } else {
            testMenu();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane CaretakerScroll;
    private javax.swing.JPanel adoptionPanel;
    private javax.swing.JPanel basicInfoPnl;
    private javax.swing.JScrollPane healthScroll;
    private javax.swing.JScrollPane incidentScroll;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    final javax.swing.JLabel lblAdopter = new javax.swing.JLabel();
    final javax.swing.JLabel lblAdoptionNotes = new javax.swing.JLabel();
    final javax.swing.JLabel lblAdoptionStatus = new javax.swing.JLabel();
    final javax.swing.JLabel lblAdoptionWhen = new javax.swing.JLabel();
    final javax.swing.JLabel lblArea = new javax.swing.JLabel();
    final javax.swing.JLabel lblBreed = new javax.swing.JLabel();
    final javax.swing.JLabel lblColor = new javax.swing.JLabel();
    final javax.swing.JLabel lblGender = new javax.swing.JLabel();
    final javax.swing.JLabel lblId = new javax.swing.JLabel();
    final javax.swing.JLabel lblName = new javax.swing.JLabel();
    private javax.swing.JTextArea taBehavior;
    private final javax.swing.JTable tblCaretakers = new javax.swing.JTable(caretakersModel);
    private final javax.swing.JTable tblHealth = new javax.swing.JTable(healthModel);
    private final javax.swing.JTable tblIncidents = new javax.swing.JTable(incidentsModel);
    // End of variables declaration//GEN-END:variables
}

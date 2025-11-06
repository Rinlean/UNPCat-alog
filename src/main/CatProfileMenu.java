package main;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import main.stuff.dbconn;

public class CatProfileMenu extends javax.swing.JFrame {

    private final int catId;
    private final DefaultTableModel healthModel = new DefaultTableModel(new Object[]{"Date", "Conditions"}, 0);
    private final DefaultTableModel caretakersModel = new DefaultTableModel(new Object[]{"Name", "Contact"}, 0);
    private final DefaultTableModel incidentsModel = new DefaultTableModel(new Object[]{"Date", "Description"}, 0);

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CatProfileMenu.class.getName());

    public CatProfileMenu(int catId) {
        this.catId = catId;
        initComponents();
        fetchAndPopulate();
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
                    JOptionPane.showMessageDialog(CatProfileMenu.this,
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
                        JOptionPane.showMessageDialog(CatProfileMenu.this,
                                "No cat found with id " + catId, "Not found", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    });
                    return;
                }
                // Extract values from ResultSet BEFORE using them inside the lambda
                final int idVal = rs.getInt("cat_id");
                final String nameVal = rs.getString("name");
                final String genderVal = rs.getString("gender");
                final String breedVal = rs.getString("breed");
                final String colorVal = rs.getString("color");
                final String areaVal = rs.getString("area_name");

                SwingUtilities.invokeLater(() -> {
                    lblId.setText(String.valueOf(idVal));
                    lblName.setText(nvl(nameVal));
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
                // Iterate all rows so multiple behavior/notes entries for the same cat are appended
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
        lblId = new javax.swing.JLabel();
        lblName = new javax.swing.JLabel();
        lblGender = new javax.swing.JLabel();
        lblBreed = new javax.swing.JLabel();
        lblColor = new javax.swing.JLabel();
        lblArea = new javax.swing.JLabel();
        adoptionPanel = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        lblAdoptionStatus = new javax.swing.JLabel();
        lblAdoptionWhen = new javax.swing.JLabel();
        lblAdoptionNotes = new javax.swing.JLabel();
        lblAdopter = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        healthScroll = new javax.swing.JScrollPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        taBehavior = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(720, 720));
        setMinimumSize(new java.awt.Dimension(720, 720));
        setPreferredSize(new java.awt.Dimension(720, 720));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        basicInfoPnl.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cat's Basic Information", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 1, 12))); // NOI18N
        basicInfoPnl.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Cat ID:");
        basicInfoPnl.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 30, -1, -1));

        jLabel2.setText("Name:");
        basicInfoPnl.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 60, -1, -1));

        jLabel3.setText("Gender:");
        basicInfoPnl.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 90, -1, -1));

        jLabel4.setText("Breed:");
        basicInfoPnl.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 120, -1, -1));

        jLabel5.setText("Color:");
        basicInfoPnl.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 150, -1, -1));

        jLabel6.setText("Area:");
        basicInfoPnl.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 180, -1, -1));

        lblId.setText("jLabel7");
        basicInfoPnl.add(lblId, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 30, -1, -1));

        lblName.setText("jLabel8");
        basicInfoPnl.add(lblName, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 60, -1, -1));

        lblGender.setText("jLabel9");
        basicInfoPnl.add(lblGender, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 90, -1, -1));

        lblBreed.setText("jLabel10");
        basicInfoPnl.add(lblBreed, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 120, -1, -1));

        lblColor.setText("jLabel11");
        basicInfoPnl.add(lblColor, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 150, -1, -1));

        lblArea.setText("jLabel12");
        basicInfoPnl.add(lblArea, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 180, -1, -1));

        getContentPane().add(basicInfoPnl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 360, 220));

        adoptionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Adoption Status", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 1, 12))); // NOI18N
        adoptionPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel13.setText("Status:");
        adoptionPanel.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 40, -1, -1));

        jLabel14.setText("When:");
        adoptionPanel.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 70, -1, -1));

        jLabel15.setText("Notes:");
        adoptionPanel.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 100, -1, -1));

        jLabel16.setText("Adopter:");
        adoptionPanel.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 130, -1, -1));

        lblAdoptionStatus.setText("jLabel17");
        adoptionPanel.add(lblAdoptionStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 40, -1, -1));

        lblAdoptionWhen.setText("jLabel18");
        adoptionPanel.add(lblAdoptionWhen, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 70, -1, -1));

        lblAdoptionNotes.setText("jLabel19");
        adoptionPanel.add(lblAdoptionNotes, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 100, -1, -1));

        lblAdopter.setText("jLabel20");
        adoptionPanel.add(lblAdopter, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 130, -1, -1));

        getContentPane().add(adoptionPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 0, 360, 220));

        tblHealth.setModel(healthModel);
        healthScroll.setViewportView(tblHealth);

        jTabbedPane1.addTab("Health", healthScroll);

        tblCaretakers.setModel(caretakersModel);
        jScrollPane2.setViewportView(tblCaretakers);

        jTabbedPane1.addTab("Caretaker", jScrollPane2);

        tblIncidents.setModel(incidentsModel);
        jScrollPane3.setViewportView(tblIncidents);

        jTabbedPane1.addTab("Incidents", jScrollPane3);

        getContentPane().add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 300, 720, 420));

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

        getContentPane().add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 220, 720, 80));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int id = -1;
        if (args.length == 0) {
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
        } else {
            try {
                id = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid cat_id argument: " + args[0], "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        final int catId = id;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            CatProfileMenu win = new CatProfileMenu(catId);
            win.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel adoptionPanel;
    private javax.swing.JPanel basicInfoPnl;
    private javax.swing.JScrollPane healthScroll;
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
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblAdopter;
    private javax.swing.JLabel lblAdoptionNotes;
    private javax.swing.JLabel lblAdoptionStatus;
    private javax.swing.JLabel lblAdoptionWhen;
    private javax.swing.JLabel lblArea;
    private javax.swing.JLabel lblBreed;
    private javax.swing.JLabel lblColor;
    private javax.swing.JLabel lblGender;
    private javax.swing.JLabel lblId;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextArea taBehavior;
    private final javax.swing.JTable tblCaretakers = new javax.swing.JTable(caretakersModel);
    private final javax.swing.JTable tblHealth = new javax.swing.JTable(healthModel);
    private final javax.swing.JTable tblIncidents = new javax.swing.JTable(incidentsModel);
    // End of variables declaration//GEN-END:variables
}

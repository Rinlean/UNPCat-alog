/*
 * CatProfileMenu.java
 *
 * Simplified Swing test UI to display a cat profile based on the unpcat_alog schema.
 * This version uses the project's dbconn helper (main.stuff.dbconn) ONLY for DB connections.
 * The adoption panel has been moved to appear under the Behavior section to avoid being
 * obscured by the Health tab content.
 *
 * Place this file in your project's src/ (adjust package if needed) and ensure
 * src/main/stuff/dbconn.java is compiled and on the classpath.
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import main.stuff.dbconn;

public class CatProfileMenu extends JFrame {

    private final int catId;

    // Basic info labels
    private final JLabel lblId = new JLabel();
    private final JLabel lblName = new JLabel();
    private final JLabel lblGender = new JLabel();
    private final JLabel lblBreed = new JLabel();
    private final JLabel lblColor = new JLabel();
    private final JLabel lblArea = new JLabel();

    // Adoption info
    private final JLabel lblAdoptionStatus = new JLabel();
    private final JLabel lblAdoptionWhen = new JLabel();
    private final JLabel lblAdoptionNotes = new JLabel();
    private final JLabel lblAdopter = new JLabel();

    // Behavior
    private final JTextArea taBehavior = new JTextArea();

    // Tables
    private final DefaultTableModel healthModel = new DefaultTableModel(new Object[]{"Date", "Conditions"}, 0);
    private final JTable tblHealth = new JTable(healthModel);

    private final DefaultTableModel caretakersModel = new DefaultTableModel(new Object[]{"Name", "Contact"}, 0);
    private final JTable tblCaretakers = new JTable(caretakersModel);

    private final DefaultTableModel incidentsModel = new DefaultTableModel(new Object[]{"Date", "Description"}, 0);
    private final JTable tblIncidents = new JTable(incidentsModel);

    public CatProfileMenu(int catId) {
        super("Cat profile - id: " + catId);
        this.catId = catId;
        initUI();
        fetchAndPopulate();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1080, 1050);
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        // Basic info panel
        JPanel basic = new JPanel(new GridLayout(0, 2, 6, 6));
        basic.setBorder(BorderFactory.createTitledBorder("Basic Info"));
        basic.add(new JLabel("ID:"));
        basic.add(lblId);
        basic.add(new JLabel("Name:"));
        basic.add(lblName);
        basic.add(new JLabel("Gender:"));
        basic.add(lblGender);
        basic.add(new JLabel("Breed:"));
        basic.add(lblBreed);
        basic.add(new JLabel("Color:"));
        basic.add(lblColor);
        basic.add(new JLabel("Area:"));
        basic.add(lblArea);

        // Adoption panel (moved to be shown under Behavior)
        JPanel adoption = new JPanel(new GridLayout(0, 2, 6, 6));
        adoption.setBorder(BorderFactory.createTitledBorder("Adoption (latest)"));
        adoption.add(new JLabel("Status:"));
        adoption.add(lblAdoptionStatus);
        adoption.add(new JLabel("When:"));
        adoption.add(lblAdoptionWhen);
        adoption.add(new JLabel("Notes:"));
        adoption.add(lblAdoptionNotes);
        adoption.add(new JLabel("Adopter:"));
        adoption.add(lblAdopter);

        // Behavior area
        taBehavior.setLineWrap(true);
        taBehavior.setWrapStyleWord(true);
        taBehavior.setEditable(false);
        JScrollPane behaviorScroll = new JScrollPane(taBehavior);
        behaviorScroll.setBorder(BorderFactory.createTitledBorder("Behavior / Personality"));

        // Health, caretakers, incidents tables in tabs
        tblHealth.setFillsViewportHeight(true);
        tblCaretakers.setFillsViewportHeight(true);
        tblIncidents.setFillsViewportHeight(true);

        JScrollPane healthScroll = new JScrollPane(tblHealth);
        JScrollPane caretakersScroll = new JScrollPane(tblCaretakers);
        JScrollPane incidentsScroll = new JScrollPane(tblIncidents);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Health", healthScroll);
        tabs.addTab("Caretakers", caretakersScroll);
        tabs.addTab("Incidents", incidentsScroll);

        // Compose left / right
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(basic, BorderLayout.NORTH);
        // left no longer contains adoption; adoption will be shown on the right under behavior

        // Right column: Behavior on top, Adoption below it
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        behaviorScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        adoption.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(behaviorScroll);
        right.add(Box.createVerticalStrut(8));
        right.add(adoption);

        right.setPreferredSize(new Dimension(480, 400));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.4);
        split.setOneTouchExpandable(true);

        add(split, BorderLayout.CENTER);
        add(tabs, BorderLayout.SOUTH);
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
                if (rs.next()) {
                    final String p = rs.getString("personality");
                    final String notes = rs.getString("notes");
                    if (p != null && !p.isEmpty()) {
                        sb.append(p).append("\n");
                    }
                    if (notes != null && !notes.isEmpty()) {
                        sb.append("Notes: ").append(notes).append("\n");
                    }
                } else {
                    sb.append("No behavior record.");
                }
                final String text = sb.toString();
                SwingUtilities.invokeLater(() -> taBehavior.setText(text));
            }
        }
    }

    private void populateHealth(Connection conn) throws SQLException {
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

    // main: prompt if no args provided (avoids abrupt exit in IDE)
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
}

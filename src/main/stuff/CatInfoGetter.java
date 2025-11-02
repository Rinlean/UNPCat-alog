package main.stuff;

import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class CatInfoGetter {

    public static class BasicInfo {

        public final int catId;
        public final String name;
        public final String gender;
        public final String breed;
        public final String color;
        public final String area;

        public BasicInfo(int catId, String name, String gender, String breed, String color, String area) {
            this.catId = catId;
            this.name = name;
            this.gender = gender;
            this.breed = breed;
            this.color = color;
            this.area = area;
        }
    }

    public static class AdoptionInfo {

        public final String status;
        public final Timestamp changedAt;
        public final String notes;
        public final Integer adopterId;
        public final String adopterName;       // may be null
        public final String adopterContact;    // may be null

        public AdoptionInfo(String status, Timestamp changedAt, String notes, Integer adopterId,
                String adopterName, String adopterContact) {
            this.status = status;
            this.changedAt = changedAt;
            this.notes = notes;
            this.adopterId = adopterId;
            this.adopterName = adopterName;
            this.adopterContact = adopterContact;
        }
    }

    private static Connection getConnection() throws Exception {
        return dbconn.getConnection();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * Fetch basic cat information. Returns null if the cat was not found.
     */
    public static BasicInfo fetchBasic(int catId) throws Exception {
        String sql = "SELECT c.cat_id, c.name, c.gender, c.breed, c.color, a.area_name "
                + "FROM cat c LEFT JOIN area a ON c.area_id = a.area_id WHERE c.cat_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int idVal = rs.getInt("cat_id");
                String nameVal = rs.getString("name");
                String genderVal = rs.getString("gender");
                String breedVal = rs.getString("breed");
                String colorVal = rs.getString("color");
                String areaVal = rs.getString("area_name");
                return new BasicInfo(idVal, nvl(nameVal), nvl(genderVal), nvl(breedVal), nvl(colorVal), nvl(areaVal));
            }
        }
    }

    /**
     * Fetch the latest adoption status row for the given cat. If there is an
     * adopter_id, this method will also attempt to resolve the adopter's
     * name/contact. Returns null if there is no adoption history for the cat.
     */
    public static AdoptionInfo fetchAdoption(int catId) throws Exception {
        String sql = "SELECT status, changed_at, notes, adopter_id FROM adoption_status WHERE cat_id = ? ORDER BY changed_at DESC LIMIT 1";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String status = rs.getString("status");
                Timestamp when = rs.getTimestamp("changed_at");
                String notes = rs.getString("notes");
                int adopterId = rs.getInt("adopter_id");
                boolean hasAdopter = !rs.wasNull();
                String adopterName = null;
                String adopterContact = null;
                if (hasAdopter) {
                    String[] adopter = lookupAdopter(conn, adopterId);
                    if (adopter != null) {
                        adopterName = adopter[0];
                        adopterContact = adopter[1];
                    }
                }
                return new AdoptionInfo(nvl(status), when, nvl(notes), hasAdopter ? adopterId : null,
                        adopterName, adopterContact);
            }
        }
    }

    /**
     * Helper to fetch adopter name/contact. Returns String[]{name, contact} or
     * null if not found. Uses provided connection (does NOT close it).
     */
    private static String[] lookupAdopter(Connection conn, int adopterId) throws SQLException {
        String sql = "SELECT name, contact_info FROM adopter WHERE adopter_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adopterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{nvl(rs.getString("name")), nvl(rs.getString("contact_info"))};
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Fetch behavior/personality text for the cat. Returns empty string if
     * none.
     */
    public static String fetchBehavior(int catId) throws Exception {
        String sql = "SELECT personality, notes FROM behavior WHERE cat_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                if (rs.next()) {
                    String p = rs.getString("personality");
                    String notes = rs.getString("notes");
                    if (p != null && !p.isEmpty()) {
                        sb.append(p).append("\n");
                    }
                    if (notes != null && !notes.isEmpty()) {
                        sb.append("Notes: ").append(notes).append("\n");
                    }
                } else {
                    // no behavior record
                }
                return sb.toString();
            }
        }
    }

    /**
     * Returns a DefaultTableModel with columns {"Date", "Conditions"}
     * containing up to 20 rows from health_record ordered by date DESC.
     */
    public static DefaultTableModel fetchHealthModel(int catId) throws Exception {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Date", "Conditions"}, 0);
        String sql = "SELECT conditions, `date` FROM health_record WHERE cat_id = ? ORDER BY `date` DESC LIMIT 20";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("date");
                    String cond = rs.getString("conditions");
                    model.addRow(new Object[]{d, nvl(cond)});
                }
            }
        } catch (SQLException e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"Error", e.getMessage()});
        }
        return model;
    }

    /**
     * Returns a DefaultTableModel with columns {"Name", "Contact"} listing
     * caretakers for the cat.
     */
    public static DefaultTableModel fetchCaretakersModel(int catId) throws Exception {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Name", "Contact"}, 0);
        String sql = "SELECT t.name, t.contact_info FROM caretaker t JOIN cat_caretaker cc ON t.caretaker_id = cc.caretaker_id WHERE cc.cat_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String contact = rs.getString("contact_info");
                    model.addRow(new Object[]{nvl(name), nvl(contact)});
                }
            }
        } catch (SQLException e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"Error", e.getMessage()});
        }
        return model;
    }

    /**
     * Returns a DefaultTableModel with columns {"Date", "Description"} listing
     * incident reports.
     */
    public static DefaultTableModel fetchIncidentsModel(int catId) throws Exception {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Date", "Description"}, 0);
        String sql = "SELECT `date`, `desc` FROM incident_report WHERE cat_id = ? ORDER BY `date` DESC LIMIT 20";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp("date");
                    String d = rs.getString("desc");
                    model.addRow(new Object[]{t, nvl(d)});
                }
            }
        } catch (SQLException e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"Error", e.getMessage()});
        }
        return model;
    }

    // Convenience methods that accept an existing Connection if callers prefer to reuse a connection:
    public static BasicInfo fetchBasic(Connection conn, int catId) throws SQLException {
        String sql = "SELECT c.cat_id, c.name, c.gender, c.breed, c.color, a.area_name "
                + "FROM cat c LEFT JOIN area a ON c.area_id = a.area_id WHERE c.cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int idVal = rs.getInt("cat_id");
                String nameVal = rs.getString("name");
                String genderVal = rs.getString("gender");
                String breedVal = rs.getString("breed");
                String colorVal = rs.getString("color");
                String areaVal = rs.getString("area_name");
                return new BasicInfo(idVal, nvl(nameVal), nvl(genderVal), nvl(breedVal), nvl(colorVal), nvl(areaVal));
            }
        }
    }

    public static AdoptionInfo fetchAdoption(Connection conn, int catId) throws SQLException {
        String sql = "SELECT status, changed_at, notes, adopter_id FROM adoption_status WHERE cat_id = ? ORDER BY changed_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String status = rs.getString("status");
                Timestamp when = rs.getTimestamp("changed_at");
                String notes = rs.getString("notes");
                int adopterId = rs.getInt("adopter_id");
                boolean hasAdopter = !rs.wasNull();
                String adopterName = null, adopterContact = null;
                if (hasAdopter) {
                    String[] adopter = lookupAdopter(conn, adopterId);
                    if (adopter != null) {
                        adopterName = adopter[0];
                        adopterContact = adopter[1];
                    }
                }
                return new AdoptionInfo(nvl(status), when, nvl(notes), hasAdopter ? adopterId : null,
                        adopterName, adopterContact);
            }
        }
    }

    public static String fetchBehavior(Connection conn, int catId) throws SQLException {
        String sql = "SELECT personality, notes FROM behavior WHERE cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                if (rs.next()) {
                    String p = rs.getString("personality");
                    String notes = rs.getString("notes");
                    if (p != null && !p.isEmpty()) {
                        sb.append(p).append("\n");
                    }
                    if (notes != null && !notes.isEmpty()) {
                        sb.append("Notes: ").append(notes).append("\n");
                    }
                }
                return sb.toString();
            }
        }
    }

    public static DefaultTableModel fetchHealthModel(Connection conn, int catId) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Date", "Conditions"}, 0);
        String sql = "SELECT conditions, `date` FROM health_record WHERE cat_id = ? ORDER BY `date` DESC LIMIT 20";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("date");
                    String cond = rs.getString("conditions");
                    model.addRow(new Object[]{d, nvl(cond)});
                }
            }
        } catch (SQLException e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"Error", e.getMessage()});
        }
        return model;
    }

    public static DefaultTableModel fetchCaretakersModel(Connection conn, int catId) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Name", "Contact"}, 0);
        String sql = "SELECT t.name, t.contact_info FROM caretaker t JOIN cat_caretaker cc ON t.caretaker_id = cc.caretaker_id WHERE cc.cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String contact = rs.getString("contact_info");
                    model.addRow(new Object[]{nvl(name), nvl(contact)});
                }
            }
        } catch (SQLException e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"Error", e.getMessage()});
        }
        return model;
    }

    public static DefaultTableModel fetchIncidentsModel(Connection conn, int catId) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Date", "Description"}, 0);
        String sql = "SELECT `date`, `desc` FROM incident_report WHERE cat_id = ? ORDER BY `date` DESC LIMIT 20";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp("date");
                    String d = rs.getString("desc");
                    model.addRow(new Object[]{t, nvl(d)});
                }
            }
        } catch (SQLException e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"Error", e.getMessage()});
        }
        return model;
    }
}



import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class CatInfoFetch {

    private static Properties loadProps() throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("DBConfig.properties")) {
            p.load(in);
        }
        return p;
    }

    public static void fetchAndPrint(int catId) {
        try {
            Properties p = loadProps();
            String url = p.getProperty("db.url");
            String user = p.getProperty("db.user");
            String pass = p.getProperty("db.password");

            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                printCatBasic(conn, catId);
                printLatestAdoption(conn, catId);
                printRecentHealth(conn, catId);
                printCaretakers(conn, catId);
                printIncidents(conn, catId);
            }
        } catch (Exception e) {
            System.err.println("Error fetching cat info: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void printCatBasic(Connection conn, int catId) throws Exception {
        String sql = "SELECT c.cat_id, c.name, c.photo_url, c.gender, c.breed, c.color, c.birth_date, c.adoption_status, a.area_name "
                   + "FROM Cat c LEFT JOIN Area a ON c.area_id = a.area_id WHERE c.cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("No cat found with cat_id = " + catId);
                    return;
                }
                System.out.println("---- Cat Basic Info ----");
                System.out.println("ID: " + rs.getInt("cat_id"));
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Gender: " + rs.getString("gender"));
                System.out.println("Breed: " + rs.getString("breed"));
                System.out.println("Color: " + rs.getString("color"));
                System.out.println("Birth date: " + rs.getDate("birth_date"));
                System.out.println("Adoption status: " + rs.getString("adoption_status"));
                System.out.println("Area: " + rs.getString("area_name"));
                System.out.println("Photo URL: " + rs.getString("photo_url"));
            }
        }
    }

    private static void printLatestAdoption(Connection conn, int catId) throws Exception {
        String sql = "SELECT status, changed_at, notes, adopter_id FROM Adoption_Status WHERE cat_id = ? ORDER BY changed_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("---- Latest Adoption Status ----");
                if (rs.next()) {
                    System.out.println("Status: " + rs.getString("status"));
                    System.out.println("Changed at: " + rs.getTimestamp("changed_at"));
                    System.out.println("Notes: " + rs.getString("notes"));
                    int adopterId = rs.getInt("adopter_id");
                    if (!rs.wasNull()) {
                        printAdopter(conn, adopterId);
                    }
                } else {
                    System.out.println("No adoption history.");
                }
            }
        }
    }

    private static void printAdopter(Connection conn, int adopterId) throws Exception {
        String sql = "SELECT name, contact_info FROM Adopter WHERE adopter_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adopterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Adopter: " + rs.getString("name") + " (" + rs.getString("contact_info") + ")");
                }
            }
        }
    }

    private static void printRecentHealth(Connection conn, int catId) throws Exception {
        String sql = "SELECT condition_desc, date FROM Health_Record WHERE cat_id = ? ORDER BY date DESC LIMIT 5";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("---- Recent Health Records ----");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.println(rs.getDate("date") + " : " + rs.getString("condition_desc"));
                }
                if (!any) System.out.println("No health records.");
            }
        }
    }

    private static void printCaretakers(Connection conn, int catId) throws Exception {
        String sql = "SELECT t.name, t.contact_info "
                   + "FROM Caretaker t JOIN Cat_Caretaker cc ON t.caretaker_id = cc.caretaker_id "
                   + "WHERE cc.cat_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("---- Caretakers ----");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.println(rs.getString("name") + " (" + rs.getString("contact_info") + ")");
                }
                if (!any) System.out.println("No caretakers assigned.");
            }
        }
    }

    private static void printIncidents(Connection conn, int catId) throws Exception {
        String sql = "SELECT date_reported, description FROM Incident_Report WHERE cat_id = ? ORDER BY date_reported DESC LIMIT 5";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("---- Recent Incidents ----");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.println(rs.getTimestamp("date_reported") + " : " + rs.getString("description"));
                }
                if (!any) System.out.println("No recent incidents.");
            }
        }
    }
}
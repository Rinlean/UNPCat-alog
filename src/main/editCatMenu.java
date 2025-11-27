package main;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import main.stuff.dbconn;
import javax.swing.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import main.stuff.QRCodeService;

public class editCatMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(editCatMenu.class.getName());

    private final Integer accountId;
    private volatile catProfileMenu profileWindow;
    private final DefaultTableModel healthModel = new DefaultTableModel(new Object[]{"ID", "Date", "Conditions"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel incidentsModel = new DefaultTableModel(new Object[]{"ID", "Date", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel BehaviourTable = new DefaultTableModel(new Object[]{"ID", "Comments"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public editCatMenu(int accountId, catProfileMenu catProf) {
        this.accountId = accountId;
        this.profileWindow = catProf;
        initComponents();

        boolean isAdmin = (Integer.valueOf(0).equals(accountId));
        ADpanel.setVisible(isAdmin);
        defaultpanel.setVisible(!isAdmin);

        SwingUtilities.invokeLater(() -> {
            try {
                if (delHealthComTable.getColumnModel().getColumnCount() > 0) {
                    delHealthComTable.getColumnModel().getColumn(0).setMinWidth(0);
                    delHealthComTable.getColumnModel().getColumn(0).setMaxWidth(0);
                    delHealthComTable.getColumnModel().getColumn(0).setPreferredWidth(0);
                    delHealthComTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                }
                if (delIncidentComTable.getColumnModel().getColumnCount() > 0) {
                    delIncidentComTable.getColumnModel().getColumn(0).setMinWidth(0);
                    delIncidentComTable.getColumnModel().getColumn(0).setMaxWidth(0);
                    delIncidentComTable.getColumnModel().getColumn(0).setPreferredWidth(0);
                    delIncidentComTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                }
                if (BehaTable.getColumnModel().getColumnCount() > 0) {
                    BehaTable.getColumnModel().getColumn(0).setMinWidth(0);
                    BehaTable.getColumnModel().getColumn(0).setMaxWidth(0);
                    BehaTable.getColumnModel().getColumn(0).setPreferredWidth(0);
                    BehaTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                }
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.FINE, "Failed to configure delete tables", t);
            }
        });

        loadAreas();
        loadCats();
        initAdoptionControls();
        SwingUtilities.invokeLater(() -> {
            Object sel = catSelector.getSelectedItem();
            int catId = 0;
            if (sel instanceof CatItem) {
                catId = ((CatItem) sel).id;
            }
            loadAvailableCaretakersForCat(catId);
            if (catId > 0) {
                populateDelHealth(catId);
                populateDelIncidents(catId);
            } else {
                healthModel.setRowCount(0);
                incidentsModel.setRowCount(0);
            }
        });
        SwingUtilities.invokeLater(this::positionNextToParent);
    }

    private void positionNextToParent() {
        try {
            if (profileWindow != null && profileWindow.isDisplayable() && profileWindow.isVisible()) {
                Point p;
                try {
                    p = profileWindow.getLocationOnScreen();
                } catch (IllegalComponentStateException e) {
                    setLocationRelativeTo(null);
                    return;
                }
                Dimension pSize = profileWindow.getSize();
                Dimension mySize = this.getSize();
                int margin = 10;

                Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

                int x = p.x + pSize.width + margin;
                int y = p.y;

                if (x + mySize.width > screen.x + screen.width) {
                    x = p.x - mySize.width - margin;
                }

                if (y + mySize.height > screen.y + screen.height) {
                    y = Math.max(screen.y, screen.y + screen.height - mySize.height - margin);
                }

                if (x < screen.x) {
                    x = screen.x;
                }
                if (y < screen.y) {
                    y = screen.y;
                }

                setLocation(x, y);
                return;
            }
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.FINE, "Failed to position next to parent", t);
        }

        setLocationRelativeTo(null);
    }

    private void loadAreas() {
        SwingUtilities.invokeLater(() -> {
            javax.swing.DefaultComboBoxModel model;
            if (areaCombo.getModel() instanceof javax.swing.DefaultComboBoxModel) {
                model = (javax.swing.DefaultComboBoxModel) areaCombo.getModel();
                model.removeAllElements();
            } else {
                model = new javax.swing.DefaultComboBoxModel();
                areaCombo.setModel(model);
            }

            model.addElement(new AreaItem(0, "None"));

            String sql = "SELECT area_id, area_name FROM area ORDER BY area_name";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addElement(new AreaItem(rs.getInt("area_id"), rs.getString("area_name")));
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.WARNING, "Failed to load areas", ex);
                JOptionPane.showMessageDialog(this, "Failed to load areas: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }

            if (model.getSize() > 0) {
                areaCombo.setSelectedIndex(0);
            }
        });
    }

    private void loadCats() {
        SwingUtilities.invokeLater(() -> {
            javax.swing.DefaultComboBoxModel model;
            if (catSelector.getModel() instanceof javax.swing.DefaultComboBoxModel) {
                model = (javax.swing.DefaultComboBoxModel) catSelector.getModel();
                model.removeAllElements();
            } else {
                model = new javax.swing.DefaultComboBoxModel();
                catSelector.setModel(model);
            }

            model.addElement(new CatItem(0, "<New Cat>"));

            boolean filterByCaretaker = (accountId != null && accountId != 0);
            String sql;
            if (filterByCaretaker) {
                sql = "SELECT c.cat_id, c.name FROM cat c JOIN cat_caretaker cc ON c.cat_id = cc.cat_id WHERE cc.caretaker_id = ? ORDER BY c.name";
            } else {
                sql = "SELECT cat_id, name FROM cat ORDER BY name";
            }

            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                if (filterByCaretaker) {
                    ps.setInt(1, accountId);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addElement(new CatItem(rs.getInt("cat_id"), rs.getString("name")));
                    }
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.WARNING, "Failed to load cats", ex);
                JOptionPane.showMessageDialog(this, "Failed to load cats: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }

            if (model.getSize() > 0) {
                catSelector.setSelectedIndex(0);
            }
        });
    }

    private void onCatSelected() {
        Object selObj = catSelector.getSelectedItem();
        CatItem item = (selObj instanceof CatItem) ? (CatItem) selObj : null;

        if (item == null) {
            return;
        }

        final int selectedCatId = item.id;

        if (selectedCatId > 0) {
            boolean hasVisibleProfile = false;
            for (Window w : Window.getWindows()) {
                if (w instanceof catProfileMenu && w.isDisplayable() && w.isVisible()) {
                    hasVisibleProfile = true;
                    synchronized (this) {
                        if (this.profileWindow == null || !this.profileWindow.isDisplayable()) {
                            this.profileWindow = (catProfileMenu) w;
                        }
                    }
                    break;
                }
            }
            if (!hasVisibleProfile) {
                openProfileForCat(selectedCatId);
            }
        }

        if (item.id == 0) {
            nameField.setText("");
            genderCombo.setSelectedItem("Unknown");
            breedField.setText("");
            colorField.setText("");
            areaCombo.setSelectedIndex(0);
            HealthTextArea.setText("");
            loadAvailableCaretakersForCat(0);
            loadDelCaretakersForCat(0);
            healthModel.setRowCount(0);
            incidentsModel.setRowCount(0);
            BehaviourTable.setRowCount(0);
            updateParentProfile(0);
        } else {
            loadCatDetails(item.id);
            loadAvailableCaretakersForCat(item.id);
            loadDelCaretakersForCat(item.id);
            populateDelHealth(item.id);
            populateDelIncidents(item.id);
            populateBehaviourTable(item.id);
            updateParentProfile(item.id);
        }
    }

    private void openProfileForCat(int catId) {
        SwingUtilities.invokeLater(() -> {
            try {
                catProfileMenu existing = this.profileWindow;
                if (existing != null && existing.isDisplayable()) {
                    if (tryInvokeRefreshOnProfile(existing, catId)) {
                        try {
                            existing.repaint();
                        } catch (Throwable ignored) {
                        }
                        return;
                    }
                }

                catProfileMenu newProf = new catProfileMenu(catId);
                this.profileWindow = newProf;
                newProf.setVisible(true);
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.FINE, "Failed to open/reuse catProfileMenu for cat " + catId, t);
            }
        });
    }

    private boolean tryInvokeRefreshOnProfile(catProfileMenu prof, int catId) {
        if (prof == null) {
            return false;
        }
        String[] candidateNames = {"setCatId", "loadCat", "refresh", "reload", "setId", "showCat"};
        Class<?> cls = prof.getClass();
        for (String name : candidateNames) {
            try {
                Method m = cls.getMethod(name, int.class);
                try {
                    m.invoke(prof, catId);
                    return true;
                } catch (Throwable invokeErr) {
                    logger.log(java.util.logging.Level.FINE, "Invocation of " + name + " failed", invokeErr);
                }
            } catch (NoSuchMethodException ignored) {
                try {
                    Method m2 = cls.getMethod(name, Integer.class);
                    try {
                        m2.invoke(prof, Integer.valueOf(catId));
                        return true;
                    } catch (Throwable invokeErr) {
                        logger.log(java.util.logging.Level.FINE, "Invocation of " + name + "(Integer) failed", invokeErr);
                    }
                } catch (NoSuchMethodException ignored2) {
                    try {
                        Method m3 = cls.getMethod(name, String.class);
                        try {
                            m3.invoke(prof, String.valueOf(catId));
                            return true;
                        } catch (Throwable invokeErr) {
                            logger.log(java.util.logging.Level.FINE, "Invocation of " + name + "(String) failed", invokeErr);
                        }
                    } catch (NoSuchMethodException ignored3) {
                    }
                }
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.FINE, "Unexpected reflection error", t);
            }
        }
        return false;
    }

    private void loadCatDetails(int catId) {
        String sql = "SELECT name, gender, breed, color, area_id FROM cat WHERE cat_id = ?";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nameField.setText(rs.getString("name"));
                    String gender = rs.getString("gender");
                    if (gender == null) {
                        gender = "Unknown";
                    }
                    genderCombo.setSelectedItem(gender);
                    breedField.setText(rs.getString("breed"));
                    colorField.setText(rs.getString("color"));
                    int areaId = rs.getInt("area_id");
                    selectAreaById(areaId);
                } else {
                    JOptionPane.showMessageDialog(this, "Cat not found (it may have been removed).", "Not found", JOptionPane.WARNING_MESSAGE);
                    loadCats();
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "Failed to load cat details", ex);
            JOptionPane.showMessageDialog(this, "Failed to load cat details: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectAreaById(int areaId) {
        javax.swing.ComboBoxModel model = areaCombo.getModel();
        if (model instanceof javax.swing.DefaultComboBoxModel) {
            javax.swing.DefaultComboBoxModel m = (javax.swing.DefaultComboBoxModel) model;
            for (int i = 0; i < m.getSize(); i++) {
                Object elem = m.getElementAt(i);
                if (elem instanceof AreaItem) {
                    AreaItem ai = (AreaItem) elem;
                    if (ai.id == areaId) {
                        areaCombo.setSelectedIndex(i);
                        return;
                    }
                }
            }
        } else {
            for (int i = 0; i < areaCombo.getItemCount(); i++) {
                Object elem = areaCombo.getItemAt(i);
                if (elem instanceof AreaItem) {
                    AreaItem ai = (AreaItem) elem;
                    if (ai.id == areaId) {
                        areaCombo.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }

        if (areaCombo.getItemCount() > 0) {
            areaCombo.setSelectedIndex(0);
        }
    }

    private void selectCatById(int id) {
        javax.swing.ComboBoxModel model = catSelector.getModel();
        if (model instanceof javax.swing.DefaultComboBoxModel) {
            javax.swing.DefaultComboBoxModel m = (javax.swing.DefaultComboBoxModel) model;
            for (int i = 0; i < m.getSize(); i++) {
                Object elem = m.getElementAt(i);
                if (elem instanceof CatItem) {
                    CatItem it = (CatItem) elem;
                    if (it.id == id) {
                        catSelector.setSelectedIndex(i);
                        return;
                    }
                }
            }
        } else {
            for (int i = 0; i < catSelector.getItemCount(); i++) {
                Object elem = catSelector.getItemAt(i);
                if (elem instanceof CatItem) {
                    CatItem it = (CatItem) elem;
                    if (it.id == id) {
                        catSelector.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }
    }

    private void updateParentProfile(int catId) {
        catProfileMenu prof = this.profileWindow;
        if (prof != null && prof.isDisplayable()) {
            if (tryInvokeRefreshOnProfile(prof, catId)) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        prof.repaint();
                    } catch (Throwable ignored) {
                    }
                });
                return;
            }
        }

        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    if (this.profileWindow != null) {
                        try {
                            this.profileWindow.dispose();
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }

                catProfileMenu newProf = new catProfileMenu(catId);
                this.profileWindow = newProf;
                newProf.setVisible(true);
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.FINE, "Failed to open fallback profile", t);
            }
        });
    }

    private void loadAvailableCaretakersForCat(int catId) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.DefaultComboBoxModel model;
            if (CaretakersCombo.getModel() instanceof javax.swing.DefaultComboBoxModel) {
                model = (javax.swing.DefaultComboBoxModel) CaretakersCombo.getModel();
                model.removeAllElements();
            } else {
                model = new javax.swing.DefaultComboBoxModel();
                CaretakersCombo.setModel(model);
            }

            model.addElement(new CaretakerItem(0, "<Select a caretaker>"));

            if (catId <= 0) {
                CaretakersCombo.setSelectedIndex(0);
                CaretakersCombo.setEnabled(false);
                return;
            }

            CaretakersCombo.setEnabled(true);

            String sql = "SELECT caretaker_id, name FROM caretaker WHERE caretaker_id NOT IN (SELECT caretaker_id FROM cat_caretaker WHERE cat_id = ?) ORDER BY name";

            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, catId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addElement(new CaretakerItem(rs.getInt("caretaker_id"), rs.getString("name")));
                    }
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.WARNING, "Failed to load caretakers", ex);
                JOptionPane.showMessageDialog(this, "Failed to load caretakers: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }

            if (model.getSize() > 0) {
                CaretakersCombo.setSelectedIndex(0);
            }
        });
    }

    private void loadDelCaretakersForCat(int catId) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.DefaultComboBoxModel model;
            if (delCaretakersCombo.getModel() instanceof javax.swing.DefaultComboBoxModel) {
                model = (javax.swing.DefaultComboBoxModel) delCaretakersCombo.getModel();
                model.removeAllElements();
            } else {
                model = new javax.swing.DefaultComboBoxModel();
                delCaretakersCombo.setModel(model);
            }

            model.addElement(new CaretakerItem(0, "<Select a caretaker to remove>"));

            if (catId <= 0) {
                delCaretakersCombo.setSelectedIndex(0);
                delCaretakersCombo.setEnabled(false);
                return;
            }

            delCaretakersCombo.setEnabled(true);

            String sql = "SELECT c.caretaker_id, c.name "
                    + "FROM caretaker c "
                    + "JOIN cat_caretaker cc ON c.caretaker_id = cc.caretaker_id "
                    + "WHERE cc.cat_id = ? "
                    + "ORDER BY c.name";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, catId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addElement(new CaretakerItem(rs.getInt("caretaker_id"), rs.getString("name")));
                    }
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.WARNING, "Failed to load associated caretakers", ex);
                SwingUtilities.invokeLater(() -> {
                    model.removeAllElements();
                    model.addElement(new CaretakerItem(0, "<Error loading caretakers>"));
                });
            }

            if (model.getSize() > 0) {
                delCaretakersCombo.setSelectedIndex(0);
            }
        });
    }

    private void populateDelHealth(int catId) {
        SwingUtilities.invokeLater(() -> {
            healthModel.setRowCount(0);
        });
        delHealthComTable.getTableHeader().setReorderingAllowed(false);
        delHealthComTable.getTableHeader().setResizingAllowed(false);
        String sql = "SELECT health_id, `date`, conditions FROM health_record WHERE cat_id = ? ORDER BY `date` DESC LIMIT 100";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("health_id");
                    Date d = rs.getDate("date");
                    String cond = rs.getString("conditions");
                    rows.add(new Object[]{id, d, cond == null ? "" : cond});
                }
                SwingUtilities.invokeLater(() -> {
                    healthModel.setRowCount(0);
                    for (Object[] r : rows) {
                        healthModel.addRow(r);
                    }
                });
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "Failed to populate health delete table", ex);
            SwingUtilities.invokeLater(() -> {
                healthModel.setRowCount(0);
                healthModel.addRow(new Object[]{-1, "Error", ex.getMessage()});
            });
        }
    }

    private void populateDelIncidents(int catId) {
        SwingUtilities.invokeLater(() -> {
            incidentsModel.setRowCount(0);
        });
        delIncidentComTable.getTableHeader().setReorderingAllowed(false);
        delIncidentComTable.getTableHeader().setResizingAllowed(false);
        String sql = "SELECT incident_id, `date`, `desc` FROM incident_report WHERE cat_id = ? ORDER BY `date` DESC LIMIT 100";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("incident_id");
                    Timestamp ts = rs.getTimestamp("date");
                    String desc = rs.getString("desc");
                    rows.add(new Object[]{id, ts, desc == null ? "" : desc});
                }
                SwingUtilities.invokeLater(() -> {
                    incidentsModel.setRowCount(0);
                    for (Object[] r : rows) {
                        incidentsModel.addRow(r);
                    }
                });
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "Failed to populate incident delete table", ex);
            SwingUtilities.invokeLater(() -> {
                incidentsModel.setRowCount(0);
                incidentsModel.addRow(new Object[]{-1, "Error", ex.getMessage()});
            });
        }
    }

    private void onAddCaretakerSelected() {
        Object o = CaretakersCombo.getSelectedItem();
        CaretakerItem ct = (o instanceof CaretakerItem) ? (CaretakerItem) o : null;
        if (ct == null || ct.id == 0) {
            addCtNameLabel.setText("Select Caretaker");
            addCtContactLabel.setText("");
            return;
        }

        addCtNameLabel.setText(ct.name == null ? "" : ct.name);

        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT contact_info FROM caretaker WHERE caretaker_id = ?")) {
            ps.setInt(1, ct.id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String contact = rs.getString("contact_info");
                    addCtContactLabel.setText(contact == null ? "" : contact);
                } else {
                    addCtContactLabel.setText("");
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.FINE, "Failed to load caretaker contact (add): " + ct.id, ex);
            addCtContactLabel.setText("");
        }
    }

    private void onDelCaretakerSelected() {
        Object o = delCaretakersCombo.getSelectedItem();
        CaretakerItem ct = (o instanceof CaretakerItem) ? (CaretakerItem) o : null;
        if (ct == null || ct.id == 0) {
            DelCtNameLabel.setText("Select Caretaker");
            DelCtContactLabel2.setText("");
            return;
        }

        DelCtNameLabel.setText(ct.name == null ? "" : ct.name);

        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT contact_info FROM caretaker WHERE caretaker_id = ?")) {
            ps.setInt(1, ct.id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String contact = rs.getString("contact_info");
                    DelCtContactLabel2.setText(contact == null ? "" : contact);
                } else {
                    DelCtContactLabel2.setText("");
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.FINE, "Failed to load caretaker contact (del): " + ct.id, ex);
            DelCtContactLabel2.setText("");
        }
    }

    private void populateStatusCombo() {
        SwingUtilities.invokeLater(() -> {
            javax.swing.DefaultComboBoxModel<String> model;
            if (statusComboBox.getModel() instanceof javax.swing.DefaultComboBoxModel) {
                model = (javax.swing.DefaultComboBoxModel<String>) statusComboBox.getModel();
                model.removeAllElements();
            } else {
                model = new javax.swing.DefaultComboBoxModel<>();
                statusComboBox.setModel(model);
            }

            model.addElement("Available");
            model.addElement("Fostered");
            model.addElement("Adopted");
            model.addElement("Missing");
            model.addElement("Other");

            if (model.getSize() > 0) {
                statusComboBox.setSelectedIndex(0);
            }
        });
    }

    private void loadOldAdopters() {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<AdopterItem> model = new DefaultListModel<>();
            String sql = "SELECT adopter_id, name, contact_info FROM adopter ORDER BY name";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addElement(new AdopterItem(rs.getInt("adopter_id"), rs.getString("name"), rs.getString("contact_info")));
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.WARNING, "Failed to load adopters", ex);
                model.clear();
                model.addElement(new AdopterItem(0, "<Error loading adopters>", ""));
            }

            @SuppressWarnings("unchecked")
            javax.swing.JList<AdopterItem> lst = (javax.swing.JList<AdopterItem>) (Object) ListofOldAdopters;
            lst.setModel(model);
            lst.clearSelection();
        });
    }

    private void selectAdopterInListById(int adopterId) {
        if (adopterId <= 0) {
            return;
        }
        @SuppressWarnings("unchecked")
        javax.swing.JList<AdopterItem> lst = (javax.swing.JList<AdopterItem>) (Object) ListofOldAdopters;
        javax.swing.ListModel<AdopterItem> model = lst.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            AdopterItem it = model.getElementAt(i);
            if (it != null && it.id == adopterId) {
                final int idx = i;
                SwingUtilities.invokeLater(() -> {
                    lst.setSelectedIndex(idx);
                    lst.ensureIndexIsVisible(idx);
                });
                return;
            }
        }
    }

    private void initAdoptionControls() {
        populateStatusCombo();
        loadOldAdopters();
        statusComboBox.addActionListener(evt -> updateAdopterInputsEnabled());
        SwingUtilities.invokeLater(this::updateAdopterInputsEnabled);
    }

    private void updateAdopterInputsEnabled() {
        String status = (statusComboBox.getSelectedItem() == null) ? "" : statusComboBox.getSelectedItem().toString().trim();
        boolean requiresAdopter = "Adopted".equalsIgnoreCase(status) || "Fostered".equalsIgnoreCase(status);

        NameAdopterField.setEnabled(requiresAdopter);
        ContactAdopterField.setEnabled(requiresAdopter);

        @SuppressWarnings("unchecked")
        javax.swing.JList<AdopterItem> adoptersList = (javax.swing.JList<AdopterItem>) (Object) ListofOldAdopters;
        adoptersList.setEnabled(requiresAdopter);

        SaveAdoptionBtn.setEnabled(true);

        if (!requiresAdopter) {
            NameAdopterField.setText("");
            ContactAdopterField.setText("");
            adoptersList.clearSelection();
        }
    }

    private void populateBehaviourTable(int catId) {
        SwingUtilities.invokeLater(() -> {
            BehaviourTable.setRowCount(0);
        });
        BehaTable.getTableHeader().setReorderingAllowed(false);
        BehaTable.getTableHeader().setResizingAllowed(false);

        if (catId <= 0) {
            SwingUtilities.invokeLater(() -> BehaviourTable.setRowCount(0));
            return;
        }

        String sql = "SELECT behavior_id, personality, notes FROM behavior WHERE cat_id = ? ORDER BY behavior_id DESC LIMIT 200";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("behavior_id");
                    String personality = rs.getString("personality");
                    String notes = rs.getString("notes");
                    String combined = "";
                    if (personality != null && !personality.isEmpty()) {
                        combined = personality;
                    }
                    if (notes != null && !notes.isEmpty()) {
                        if (!combined.isEmpty()) {
                            combined += " — ";
                        }
                        combined += notes;
                    }
                    rows.add(new Object[]{id, combined});
                }
                SwingUtilities.invokeLater(() -> {
                    BehaviourTable.setRowCount(0);
                    for (Object[] r : rows) {
                        BehaviourTable.addRow(r);
                    }
                });
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "Failed to populate behaviour table", ex);
            SwingUtilities.invokeLater(() -> {
                BehaviourTable.setRowCount(0);
                BehaviourTable.addRow(new Object[]{-1, "Error", ex.getMessage()});
            });
        }
    }

    private static final class CatItem {

        final int id;
        final String name;

        CatItem(int id, String name) {
            this.id = id;
            this.name = (name == null) ? "" : name;
        }

        @Override
        public String toString() {
            if (id == 0) {
                return name;
            }
            return id + " - " + name;
        }
    }

    private static final class AreaItem {

        final int id;
        final String name;

        AreaItem(int id, String name) {
            this.id = id;
            this.name = (name == null) ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class CaretakerItem {

        final int id;
        final String name;

        CaretakerItem(int id, String name) {
            this.id = id;
            this.name = (name == null) ? "" : name;
        }

        @Override
        public String toString() {
            if (id == 0) {
                return name;
            }
            return id + " - " + name;
        }
    }

    private static final class AdopterItem {

        final int id;
        final String name;
        final String contact;

        AdopterItem(int id, String name, String contact) {
            this.id = id;
            this.name = (name == null) ? "" : name;
            this.contact = (contact == null) ? "" : contact;
        }

        @Override
        public String toString() {
            if (id == 0) {
                return name;
            }
            if (contact == null || contact.isEmpty()) {
                return name;
            }
            return name + " (" + contact + ")";
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        CommentsPanel = new javax.swing.JTabbedPane();
        healthPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        HealthTextArea = new javax.swing.JTextArea();
        AddHealthCommentBtn = new javax.swing.JButton();
        incidentsPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        IncidentsTextArea = new javax.swing.JTextArea();
        AddIncidentCommentBtn = new javax.swing.JButton();
        delCommentsPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        delIncidentComTable = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        delHealthComTable = new javax.swing.JTable();
        DeleteCommentsButton = new javax.swing.JButton();
        InfoPanel = new javax.swing.JTabbedPane();
        BasicInfoPanel = new javax.swing.JPanel();
        colorField = new javax.swing.JTextField();
        breedField = new javax.swing.JTextField();
        nameField = new javax.swing.JTextField();
        areaCombo = new javax.swing.JComboBox<>();
        catSelector = new javax.swing.JComboBox<>();
        genderCombo = new javax.swing.JComboBox<>();
        saveBtn = new javax.swing.JButton();
        AdoptionPanel = new javax.swing.JPanel();
        statusComboBox = new javax.swing.JComboBox<>();
        jScrollPane5 = new javax.swing.JScrollPane();
        NotesAdoptionArea = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        ListofOldAdopters = new javax.swing.JList<>();
        NameAdopterField = new javax.swing.JTextField();
        ContactAdopterField = new javax.swing.JTextField();
        SaveAdoptionBtn = new javax.swing.JButton();
        BehaviourPanel = new javax.swing.JPanel();
        BehInnerPanel = new javax.swing.JTabbedPane();
        BehavComments = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        BehaviourArea = new javax.swing.JTextArea();
        SvBehavCommentsBtn = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        OthernoteArea = new javax.swing.JTextArea();
        deleteBehaviourNotes = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        BehaTable = new javax.swing.JTable();
        DelBehaNotesCommentsBtn = new javax.swing.JButton();
        caretakerPanel = new javax.swing.JLayeredPane();
        ADpanel = new javax.swing.JPanel();
        DelCaretakerPanel = new javax.swing.JPanel();
        delCaretakerBtn = new javax.swing.JButton();
        delCaretakersCombo = new javax.swing.JComboBox<>();
        DelCtNameLabel = new javax.swing.JLabel();
        DelCtContactLabel2 = new javax.swing.JLabel();
        AddCaretakerPanel = new javax.swing.JPanel();
        CaretakersCombo = new javax.swing.JComboBox<>();
        addCaretakerBtn = new javax.swing.JButton();
        addCtContactLabel = new javax.swing.JLabel();
        addCtNameLabel = new javax.swing.JLabel();
        defaultpanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Cat Menu");
        setMaximumSize(new java.awt.Dimension(420, 740));
        setPreferredSize(new java.awt.Dimension(415, 780));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        healthPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        HealthTextArea.setColumns(20);
        HealthTextArea.setRows(5);
        HealthTextArea.setBorder(javax.swing.BorderFactory.createTitledBorder("Health Comment"));
        jScrollPane1.setViewportView(HealthTextArea);

        healthPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 18, 380, 233));

        AddHealthCommentBtn.setText("Comment");
        AddHealthCommentBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddHealthCommentBtnActionPerformed(evt);
            }
        });
        healthPanel.add(AddHealthCommentBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(288, 257, -1, -1));

        CommentsPanel.addTab("Health", healthPanel);

        incidentsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        IncidentsTextArea.setColumns(20);
        IncidentsTextArea.setRows(5);
        IncidentsTextArea.setBorder(javax.swing.BorderFactory.createTitledBorder("Incident Comment"));
        jScrollPane2.setViewportView(IncidentsTextArea);

        incidentsPanel.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 18, 380, 233));

        AddIncidentCommentBtn.setText("Comment");
        AddIncidentCommentBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddIncidentCommentBtnActionPerformed(evt);
            }
        });
        incidentsPanel.add(AddIncidentCommentBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(285, 257, -1, -1));

        CommentsPanel.addTab("Incidents", incidentsPanel);

        delCommentsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane3.setBorder(javax.swing.BorderFactory.createTitledBorder("Incident Comments"));

        delIncidentComTable.setModel(incidentsModel);
        jScrollPane3.setViewportView(delIncidentComTable);

        delCommentsPanel.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 140, 380, 123));

        jScrollPane4.setBorder(javax.swing.BorderFactory.createTitledBorder("Health Comments"));

        delHealthComTable.setModel(healthModel);
        jScrollPane4.setViewportView(delHealthComTable);

        delCommentsPanel.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 13, 380, 123));

        DeleteCommentsButton.setText("Delete");
        DeleteCommentsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteCommentsButtonActionPerformed(evt);
            }
        });
        delCommentsPanel.add(DeleteCommentsButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 270, -1, -1));

        CommentsPanel.addTab("Delete Comments", delCommentsPanel);

        getContentPane().add(CommentsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 400, 400, 340));

        BasicInfoPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        colorField.setBorder(javax.swing.BorderFactory.createTitledBorder("Color"));
        BasicInfoPanel.add(colorField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, 340, -1));

        breedField.setBorder(javax.swing.BorderFactory.createTitledBorder("Breed"));
        BasicInfoPanel.add(breedField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, 340, -1));

        nameField.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        BasicInfoPanel.add(nameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 340, -1));

        areaCombo.setModel(new javax.swing.DefaultComboBoxModel<>());
        areaCombo.setBorder(javax.swing.BorderFactory.createTitledBorder("Area"));
        BasicInfoPanel.add(areaCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 270, 340, -1));

        catSelector.setModel(new javax.swing.DefaultComboBoxModel());
        catSelector.setBorder(javax.swing.BorderFactory.createTitledBorder("Cats"));
        catSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                catSelectorActionPerformed(evt);
            }
        });
        BasicInfoPanel.add(catSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 30, 190, -1));

        genderCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Male", "Female", "Unknown"}));
        genderCombo.setBorder(javax.swing.BorderFactory.createTitledBorder("Gender"));
        BasicInfoPanel.add(genderCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 30, 130, -1));

        saveBtn.setText("Save");
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBtnActionPerformed(evt);
            }
        });
        BasicInfoPanel.add(saveBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 330, -1, -1));

        InfoPanel.addTab("Basic Info", BasicInfoPanel);

        AdoptionPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        statusComboBox.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));
        AdoptionPanel.add(statusComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, 170, -1));

        NotesAdoptionArea.setColumns(20);
        NotesAdoptionArea.setRows(5);
        NotesAdoptionArea.setBorder(javax.swing.BorderFactory.createTitledBorder("Notes"));
        jScrollPane5.setViewportView(NotesAdoptionArea);

        AdoptionPanel.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 60, 340, -1));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Choose or Add new Adopter"));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ListofOldAdopters.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Available Adopters", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 10))); // NOI18N
        ListofOldAdopters.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane6.setViewportView(ListofOldAdopters);

        jPanel1.add(jScrollPane6, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 30, 110, 140));

        NameAdopterField.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        jPanel1.add(NameAdopterField, new org.netbeans.lib.awtextra.AbsoluteConstraints(135, 30, 210, -1));

        ContactAdopterField.setBorder(javax.swing.BorderFactory.createTitledBorder("Email"));
        jPanel1.add(ContactAdopterField, new org.netbeans.lib.awtextra.AbsoluteConstraints(135, 80, 210, -1));

        SaveAdoptionBtn.setText("Save");
        SaveAdoptionBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveAdoptionBtnActionPerformed(evt);
            }
        });
        jPanel1.add(SaveAdoptionBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 140, -1, -1));

        AdoptionPanel.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, 360, 180));

        InfoPanel.addTab("Adoption", AdoptionPanel);

        BehaviourPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BehaviourArea.setColumns(20);
        BehaviourArea.setRows(5);
        BehaviourArea.setBorder(javax.swing.BorderFactory.createTitledBorder("Behaviour Comment"));
        jScrollPane7.setViewportView(BehaviourArea);

        SvBehavCommentsBtn.setText("Comment");
        SvBehavCommentsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SvBehavCommentsBtnActionPerformed(evt);
            }
        });

        OthernoteArea.setColumns(20);
        OthernoteArea.setRows(5);
        OthernoteArea.setBorder(javax.swing.BorderFactory.createTitledBorder("Note Comment"));
        jScrollPane8.setViewportView(OthernoteArea);

        javax.swing.GroupLayout BehavCommentsLayout = new javax.swing.GroupLayout(BehavComments);
        BehavComments.setLayout(BehavCommentsLayout);
        BehavCommentsLayout.setHorizontalGroup(
            BehavCommentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BehavCommentsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(BehavCommentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BehavCommentsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(SvBehavCommentsBtn)
                        .addGap(22, 22, 22))
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE))
                .addContainerGap())
        );
        BehavCommentsLayout.setVerticalGroup(
            BehavCommentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BehavCommentsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addComponent(SvBehavCommentsBtn)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        BehInnerPanel.addTab("Behaviour Comments", BehavComments);

        deleteBehaviourNotes.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane9.setBorder(javax.swing.BorderFactory.createTitledBorder("Behaviour Comments"));

        BehaTable.setModel(BehaviourTable);
        jScrollPane9.setViewportView(BehaTable);

        deleteBehaviourNotes.add(jScrollPane9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 380, 250));

        DelBehaNotesCommentsBtn.setText("Delete");
        DelBehaNotesCommentsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DelBehaNotesCommentsBtnActionPerformed(evt);
            }
        });
        deleteBehaviourNotes.add(DelBehaNotesCommentsBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 270, -1, -1));

        BehInnerPanel.addTab("Delete", deleteBehaviourNotes);

        BehaviourPanel.add(BehInnerPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 360));

        InfoPanel.addTab("Behaviour", BehaviourPanel);

        caretakerPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ADpanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        DelCaretakerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Remove Caretaker"));
        DelCaretakerPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        delCaretakerBtn.setText("Remove");
        delCaretakerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delCaretakerBtnActionPerformed(evt);
            }
        });
        DelCaretakerPanel.add(delCaretakerBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 30, -1, -1));

        delCaretakersCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        delCaretakersCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delCaretakersComboActionPerformed(evt);
            }
        });
        DelCaretakerPanel.add(delCaretakersCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 30, 220, -1));

        DelCtNameLabel.setText("Select Caretaker");
        DelCtNameLabel.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        DelCaretakerPanel.add(DelCtNameLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 300, -1));

        DelCtContactLabel2.setText("Select Caretaker");
        DelCtContactLabel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Contact"));
        DelCaretakerPanel.add(DelCtContactLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, 300, -1));

        ADpanel.add(DelCaretakerPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, 350, 160));

        AddCaretakerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Add Caretaker"));
        AddCaretakerPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        CaretakersCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        CaretakersCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CaretakersComboActionPerformed(evt);
            }
        });
        AddCaretakerPanel.add(CaretakersCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 30, 220, -1));

        addCaretakerBtn.setText("Add");
        addCaretakerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCaretakerBtnActionPerformed(evt);
            }
        });
        AddCaretakerPanel.add(addCaretakerBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 30, -1, -1));

        addCtContactLabel.setText("Select Caretaker");
        addCtContactLabel.setBorder(javax.swing.BorderFactory.createTitledBorder("Contact"));
        AddCaretakerPanel.add(addCtContactLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, 300, -1));

        addCtNameLabel.setText("Select Caretaker");
        addCtNameLabel.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        AddCaretakerPanel.add(addCtNameLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 300, -1));

        ADpanel.add(AddCaretakerPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 350, 160));

        caretakerPanel.setLayer(ADpanel, javax.swing.JLayeredPane.DRAG_LAYER);
        caretakerPanel.add(ADpanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 360));

        jLabel1.setText("Contact Admin to Add or Remove Co-Caretakers");

        javax.swing.GroupLayout defaultpanelLayout = new javax.swing.GroupLayout(defaultpanel);
        defaultpanel.setLayout(defaultpanelLayout);
        defaultpanelLayout.setHorizontalGroup(
            defaultpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(defaultpanelLayout.createSequentialGroup()
                .addGap(71, 71, 71)
                .addComponent(jLabel1)
                .addContainerGap(71, Short.MAX_VALUE))
        );
        defaultpanelLayout.setVerticalGroup(
            defaultpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(defaultpanelLayout.createSequentialGroup()
                .addGap(167, 167, 167)
                .addComponent(jLabel1)
                .addContainerGap(177, Short.MAX_VALUE))
        );

        caretakerPanel.add(defaultpanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 360));

        InfoPanel.addTab("Caretaker", caretakerPanel);

        getContentPane().add(InfoPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 400));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void saveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "No cat selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String gender = (String) genderCombo.getSelectedItem();
        String breed = breedField.getText().trim();
        String color = colorField.getText().trim();
        Object areaObj = areaCombo.getSelectedItem();
        int areaId = 0;
        if (areaObj instanceof AreaItem) {
            areaId = ((AreaItem) areaObj).id;
        }

        if (sel.id == 0) {
            String insertSql = "INSERT INTO cat (name, gender, breed, color, area_id) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, gender);
                ps.setString(3, breed.isEmpty() ? null : breed);
                ps.setString(4, color.isEmpty() ? null : color);
                if (areaId == 0) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, areaId);
                }
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Insert failed, no rows affected.");
                }
                int newId = -1;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                    }
                }

                if (accountId != null && accountId != 0 && newId > 0) {
                    String assocSql = "INSERT INTO cat_caretaker (cat_id, caretaker_id) VALUES (?, ?)";
                    try (PreparedStatement ps2 = conn.prepareStatement(assocSql)) {
                        ps2.setInt(1, newId);
                        ps2.setInt(2, accountId);
                        ps2.executeUpdate();
                    } catch (SQLException assocEx) {
                        logger.log(java.util.logging.Level.WARNING, "Failed to create cat_caretaker association", assocEx);
                    }
                }

                JOptionPane.showMessageDialog(this, "Cat added (id=" + newId + ").", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadCats();
                if (newId > 0) {
                    selectCatById(newId);
                }
                updateParentProfile(newId);

                if (accountId != null && accountId != 0 && newId > 0) {
                    final int generatedId = newId;
                    final String generatedName = name;
                    new SwingWorker<Path, Void>() {
                        @Override
                        protected Path doInBackground() throws Exception {
                            QRCodeService svc = new QRCodeService("");
                            return svc.generateQRCodeForCat(generatedId, generatedName, true);
                        }

                        @Override
                        protected void done() {
                            try {
                                Path saved = get();
                                if (saved != null) {
                                    JOptionPane.showMessageDialog(editCatMenu.this,
                                            "QR code saved to: " + saved.toAbsolutePath(),
                                            "QR Generated",
                                            JOptionPane.INFORMATION_MESSAGE);
                                }
                            } catch (Exception ex) {
                                logger.log(java.util.logging.Level.FINE, "Failed to generate QR for cat " + generatedId, ex);
                                JOptionPane.showMessageDialog(editCatMenu.this,
                                        "Cat added but QR generation failed: " + ex.getMessage(),
                                        "QR generation error",
                                        JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    }.execute();
                }

            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to insert cat", ex);
                JOptionPane.showMessageDialog(this, "Failed to insert cat: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            String updateSql = "UPDATE cat SET name = ?, gender = ?, breed = ?, color = ?, area_id = ? WHERE cat_id = ?";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, name);
                ps.setString(2, gender);
                ps.setString(3, breed.isEmpty() ? null : breed);
                ps.setString(4, color.isEmpty() ? null : color);
                if (areaId == 0) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, areaId);
                }
                ps.setInt(6, sel.id);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    JOptionPane.showMessageDialog(this, "No rows updated. The cat may have been removed.", "Warning", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Cat updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadCats();
                    selectCatById(sel.id);
                    updateParentProfile(sel.id);
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to update cat", ex);
                JOptionPane.showMessageDialog(this, "Failed to update cat: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_saveBtnActionPerformed

    private void catSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_catSelectorActionPerformed
        onCatSelected();
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            saveBtn.setText("Add");
        } else {
            saveBtn.setText("Save");
        }
    }//GEN-LAST:event_catSelectorActionPerformed

    private void AddHealthCommentBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddHealthCommentBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat before adding a health comment.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String comment = HealthTextArea.getText();
        if (comment == null || comment.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type the health comment in the text area before clicking Comment.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        comment = comment.trim();

        String insertSql = "INSERT INTO health_record (cat_id, conditions, date) VALUES (?, ?, CURRENT_DATE())";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, sel.id);
            ps.setString(2, comment);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                JOptionPane.showMessageDialog(this, "Failed to add health comment.", "DB error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Health comment added.", "Success", JOptionPane.INFORMATION_MESSAGE);
                HealthTextArea.setText("");
                populateDelHealth(sel.id);
                try {
                    updateParentProfile(sel.id);
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.FINE, "Failed to update profile after adding health comment", t);
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to insert health comment", ex);
            JOptionPane.showMessageDialog(this, "Failed to add health comment: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_AddHealthCommentBtnActionPerformed

    private void AddIncidentCommentBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddIncidentCommentBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat before adding an incident comment.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String comment = IncidentsTextArea.getText();
        if (comment == null || comment.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type the incident description in the text area before clicking Comment.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        comment = comment.trim();

        String insertSql = "INSERT INTO incident_report (cat_id, date, `desc`) VALUES (?, NOW(), ?)";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, sel.id);
            ps.setString(2, comment);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                JOptionPane.showMessageDialog(this, "Failed to add incident report.", "DB error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Incident report added.", "Success", JOptionPane.INFORMATION_MESSAGE);
                IncidentsTextArea.setText("");
                populateDelIncidents(sel.id);
                try {
                    updateParentProfile(sel.id);
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.FINE, "Failed to update profile after adding incident", t);
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to insert incident report", ex);
            JOptionPane.showMessageDialog(this, "Failed to add incident report: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_AddIncidentCommentBtnActionPerformed

    private void delCaretakerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delCaretakerBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        Object cObj = delCaretakersCombo.getSelectedItem();
        CaretakerItem ct = (cObj instanceof CaretakerItem) ? (CaretakerItem) cObj : null;

        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ct == null || ct.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a caretaker to remove.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove caretaker \"" + ct.name + "\" from cat \"" + sel.name + "\"?",
                "Confirm remove",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM cat_caretaker WHERE cat_id = ? AND caretaker_id = ?";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sel.id);
            ps.setInt(2, ct.id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Caretaker removed.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAvailableCaretakersForCat(sel.id);
                loadDelCaretakersForCat(sel.id);
                updateParentProfile(sel.id);
            } else {
                JOptionPane.showMessageDialog(this, "No association removed (it may have already been removed).", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadAvailableCaretakersForCat(sel.id);
                loadDelCaretakersForCat(sel.id);
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "Failed to remove caretaker association", ex);
            JOptionPane.showMessageDialog(this, "Failed to remove caretaker: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            loadAvailableCaretakersForCat(sel.id);
            loadDelCaretakersForCat(sel.id);
        }
    }//GEN-LAST:event_delCaretakerBtnActionPerformed

    private void DeleteCommentsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteCommentsButtonActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int catId = sel.id;

        int[] selectedHealthRows = delHealthComTable.getSelectedRows();
        List<Integer> healthIds = new ArrayList<>();
        for (int viewRow : selectedHealthRows) {
            int modelRow = delHealthComTable.convertRowIndexToModel(viewRow);
            Object val = healthModel.getValueAt(modelRow, 0);
            if (val instanceof Number) {
                healthIds.add(((Number) val).intValue());
            } else if (val != null) {
                try {
                    healthIds.add(Integer.parseInt(val.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int[] selectedIncidentRows = delIncidentComTable.getSelectedRows();
        List<Integer> incidentIds = new ArrayList<>();
        for (int viewRow : selectedIncidentRows) {
            int modelRow = delIncidentComTable.convertRowIndexToModel(viewRow);
            Object val = incidentsModel.getValueAt(modelRow, 0);
            if (val instanceof Number) {
                incidentIds.add(((Number) val).intValue());
            } else if (val != null) {
                try {
                    incidentIds.add(Integer.parseInt(val.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (healthIds.isEmpty() && incidentIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select one or more comments to delete.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int total = healthIds.size() + incidentIds.size();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + total + " selected comment(s)? This cannot be undone.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int deletedHealth = 0;
        int deletedIncidents = 0;

        try (Connection conn = dbconn.getConnection()) {
            try {
                conn.setAutoCommit(false);

                if (!healthIds.isEmpty()) {
                    String deleteHealthSql = "DELETE FROM health_record WHERE health_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteHealthSql)) {
                        for (Integer id : healthIds) {
                            ps.setInt(1, id);
                            deletedHealth += ps.executeUpdate();
                        }
                    }
                }

                if (!incidentIds.isEmpty()) {
                    String deleteIncSql = "DELETE FROM incident_report WHERE incident_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteIncSql)) {
                        for (Integer id : incidentIds) {
                            ps.setInt(1, id);
                            deletedIncidents += ps.executeUpdate();
                        }
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (Throwable ignored) {
                }
                throw ex;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (Throwable ignored) {
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to delete comments", ex);
            JOptionPane.showMessageDialog(this, "Failed to delete comments: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            populateDelHealth(catId);
            populateDelIncidents(catId);
            return;
        }

        String msg = "Deleted " + deletedHealth + " health comment(s), " + deletedIncidents + " incident(s).";
        JOptionPane.showMessageDialog(this, msg, "Deleted", JOptionPane.INFORMATION_MESSAGE);
        populateDelHealth(catId);
        populateDelIncidents(catId);

        try {
            updateParentProfile(catId);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.FINE, "Failed to update profile after deleting comments", t);
        }
    }//GEN-LAST:event_DeleteCommentsButtonActionPerformed

    private void addCaretakerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCaretakerBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        Object caretakersComboObj = null;
        try {
            caretakersComboObj = this.getClass().getDeclaredField("CaretakersCombo") != null ? CaretakersCombo.getSelectedItem() : null;
        } catch (Throwable ignored) {
        }
        Object cObj = caretakersComboObj;
        CaretakerItem ct = (cObj instanceof CaretakerItem) ? (CaretakerItem) cObj : null;

        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select an existing cat before adding a caretaker.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ct == null || ct.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a caretaker to add.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String insertSql = "INSERT INTO cat_caretaker (cat_id, caretaker_id) VALUES (?, ?)";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, sel.id);
            ps.setInt(2, ct.id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Caretaker added to cat.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAvailableCaretakersForCat(sel.id);
                loadDelCaretakersForCat(sel.id);
                updateParentProfile(sel.id);
            } else {
                JOptionPane.showMessageDialog(this, "No association created.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "Failed to insert cat_caretaker", ex);
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("constraint")) {
                JOptionPane.showMessageDialog(this, "That caretaker is already associated with this cat.", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadAvailableCaretakersForCat(sel.id);
                loadDelCaretakersForCat(sel.id);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add caretaker: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_addCaretakerBtnActionPerformed

    private void CaretakersComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CaretakersComboActionPerformed
        onAddCaretakerSelected();
    }//GEN-LAST:event_CaretakersComboActionPerformed

    private void delCaretakersComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delCaretakersComboActionPerformed
        onDelCaretakerSelected();
    }//GEN-LAST:event_delCaretakersComboActionPerformed

    private void SaveAdoptionBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAdoptionBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int catId = sel.id;

        String status = (statusComboBox.getSelectedItem() == null) ? "Other" : statusComboBox.getSelectedItem().toString();
        String notes = NotesAdoptionArea.getText();
        if (notes == null) {
            notes = "";
        }
        
        boolean requiresAdopter = "Adopted".equalsIgnoreCase(status.trim()) || "Fostered".equalsIgnoreCase(status.trim());

        @SuppressWarnings("unchecked")
        javax.swing.JList<AdopterItem> adoptersList = (javax.swing.JList<AdopterItem>) (Object) ListofOldAdopters;
        AdopterItem selectedAdopter = (adoptersList.getSelectedValue() instanceof AdopterItem) ? adoptersList.getSelectedValue() : null;

        String newName = NameAdopterField.getText() == null ? "" : NameAdopterField.getText().trim();
        String newContact = ContactAdopterField.getText() == null ? "" : ContactAdopterField.getText().trim();

        if (requiresAdopter) {
            if (selectedAdopter == null && newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please either select an existing adopter or enter adopter's name.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            selectedAdopter = null;
            newName = "";
            newContact = "";
        }

        Integer adopterIdToUse = null;
        if (requiresAdopter && selectedAdopter != null && selectedAdopter.id > 0) {
            adopterIdToUse = selectedAdopter.id;
        }

        if (requiresAdopter && adopterIdToUse == null) {
            String insertAdopterSql = "INSERT INTO adopter (name, contact_info) VALUES (?, ?)";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertAdopterSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, newName);
                ps.setString(2, newContact.isEmpty() ? null : newContact);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Failed to insert adopter");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        adopterIdToUse = keys.getInt(1);
                    }
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to insert adopter", ex);
                JOptionPane.showMessageDialog(this, "Failed to add adopter: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String insertStatusSql = "INSERT INTO adoption_status (cat_id, status, changed_at, notes, adopter_id) VALUES (?, ?, NOW(), ?, ?)";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertStatusSql)) {
            ps.setInt(1, catId);
            ps.setString(2, status);
            ps.setString(3, notes.isEmpty() ? null : notes);
            if (adopterIdToUse != null) {
                ps.setInt(4, adopterIdToUse);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Failed to insert adoption status");
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to save adoption status", ex);
            JOptionPane.showMessageDialog(this, "Failed to save adoption: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Adoption status saved.", "Success", JOptionPane.INFORMATION_MESSAGE);
        loadOldAdopters();
        if (adopterIdToUse != null) {
            selectAdopterInListById(adopterIdToUse);
        }

        try {
            loadAvailableCaretakersForCat(catId);
            loadDelCaretakersForCat(catId);
            populateDelHealth(catId);
            populateDelIncidents(catId);
            updateParentProfile(catId);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.FINE, "Failed to refresh after saving adoption", t);
        }
        
        SwingUtilities.invokeLater(() -> {
            NameAdopterField.setText("");
            ContactAdopterField.setText("");
            NotesAdoptionArea.setText("");
            if (statusComboBox.getItemCount() > 0) {
                statusComboBox.setSelectedIndex(0);
            }
            @SuppressWarnings("unchecked")
            javax.swing.JList<AdopterItem> adopterList = (javax.swing.JList<AdopterItem>) (Object) ListofOldAdopters;
            adopterList.clearSelection();
            NameAdopterField.requestFocusInWindow();
            updateAdopterInputsEnabled();
        });
    }//GEN-LAST:event_SaveAdoptionBtnActionPerformed

    private void DelBehaNotesCommentsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DelBehaNotesCommentsBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int catId = sel.id;

        int[] selectedRows = BehaTable.getSelectedRows();
        List<Integer> ids = new ArrayList<>();
        for (int viewRow : selectedRows) {
            int modelRow = BehaTable.convertRowIndexToModel(viewRow);
            Object val = BehaviourTable.getValueAt(modelRow, 0);
            if (val instanceof Number) {
                ids.add(((Number) val).intValue());
            } else if (val != null) {
                try {
                    ids.add(Integer.parseInt(val.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select one or more behaviour comments to delete.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + ids.size() + " selected behaviour comment(s)? This cannot be undone.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int deleted = 0;
        try (Connection conn = dbconn.getConnection()) {
            try {
                conn.setAutoCommit(false);
                String deleteSql = "DELETE FROM behavior WHERE behavior_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    for (Integer id : ids) {
                        ps.setInt(1, id);
                        deleted += ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (Throwable ignored) {
                }
                throw ex;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (Throwable ignored) {
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to delete behaviour comments", ex);
            JOptionPane.showMessageDialog(this, "Failed to delete behaviour comments: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            populateBehaviourTable(catId);
            return;
        }

        JOptionPane.showMessageDialog(this, "Deleted " + deleted + " behaviour comment(s).", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        populateBehaviourTable(catId);
        try {
            updateParentProfile(catId);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.FINE, "Failed to update profile after deleting behaviour comments", t);
        }
    }//GEN-LAST:event_DelBehaNotesCommentsBtnActionPerformed

    private void SvBehavCommentsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SvBehavCommentsBtnActionPerformed
        Object selObj = catSelector.getSelectedItem();
        CatItem sel = (selObj instanceof CatItem) ? (CatItem) selObj : null;
        if (sel == null || sel.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat before adding a behaviour comment.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String personality = BehaviourArea.getText();
        String notes = OthernoteArea.getText();

        if ((personality == null || personality.trim().isEmpty()) && (notes == null || notes.trim().isEmpty())) {
            JOptionPane.showMessageDialog(this, "Please type the behaviour or notes in the text areas before clicking Comment.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        personality = personality == null ? "" : personality.trim();
        notes = notes == null ? "" : notes.trim();

        String insertSql = "INSERT INTO behavior (cat_id, personality, notes) VALUES (?, ?, ?)";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, sel.id);
            ps.setString(2, personality.isEmpty() ? null : personality);
            ps.setString(3, notes.isEmpty() ? null : notes);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                JOptionPane.showMessageDialog(this, "Failed to add behaviour comment.", "DB error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Behaviour comment added.", "Success", JOptionPane.INFORMATION_MESSAGE);
                BehaviourArea.setText("");
                OthernoteArea.setText("");
                populateBehaviourTable(sel.id);
                try {
                    updateParentProfile(sel.id);
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.FINE, "Failed to update profile after adding behaviour comment", t);
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to insert behaviour comment", ex);
            JOptionPane.showMessageDialog(this, "Failed to add behaviour comment: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_SvBehavCommentsBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ADpanel;
    private javax.swing.JPanel AddCaretakerPanel;
    private javax.swing.JButton AddHealthCommentBtn;
    private javax.swing.JButton AddIncidentCommentBtn;
    private javax.swing.JPanel AdoptionPanel;
    private javax.swing.JPanel BasicInfoPanel;
    private javax.swing.JTabbedPane BehInnerPanel;
    private javax.swing.JTable BehaTable;
    private javax.swing.JPanel BehavComments;
    private javax.swing.JTextArea BehaviourArea;
    private javax.swing.JPanel BehaviourPanel;
    private javax.swing.JComboBox<String> CaretakersCombo;
    private javax.swing.JTabbedPane CommentsPanel;
    private javax.swing.JTextField ContactAdopterField;
    private javax.swing.JButton DelBehaNotesCommentsBtn;
    private javax.swing.JPanel DelCaretakerPanel;
    private javax.swing.JLabel DelCtContactLabel2;
    private javax.swing.JLabel DelCtNameLabel;
    private javax.swing.JButton DeleteCommentsButton;
    private javax.swing.JTextArea HealthTextArea;
    private javax.swing.JTextArea IncidentsTextArea;
    private javax.swing.JTabbedPane InfoPanel;
    private javax.swing.JList<String> ListofOldAdopters;
    private javax.swing.JTextField NameAdopterField;
    private javax.swing.JTextArea NotesAdoptionArea;
    private javax.swing.JTextArea OthernoteArea;
    private javax.swing.JButton SaveAdoptionBtn;
    private javax.swing.JButton SvBehavCommentsBtn;
    private javax.swing.JButton addCaretakerBtn;
    private javax.swing.JLabel addCtContactLabel;
    private javax.swing.JLabel addCtNameLabel;
    private javax.swing.JComboBox<String> areaCombo;
    private javax.swing.JTextField breedField;
    private javax.swing.JLayeredPane caretakerPanel;
    private javax.swing.JComboBox<String> catSelector;
    private javax.swing.JTextField colorField;
    private javax.swing.JPanel defaultpanel;
    private javax.swing.JButton delCaretakerBtn;
    private javax.swing.JComboBox<String> delCaretakersCombo;
    private javax.swing.JPanel delCommentsPanel;
    private javax.swing.JTable delHealthComTable;
    private javax.swing.JTable delIncidentComTable;
    private javax.swing.JPanel deleteBehaviourNotes;
    private javax.swing.JComboBox<String> genderCombo;
    private javax.swing.JPanel healthPanel;
    private javax.swing.JPanel incidentsPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTextField nameField;
    private javax.swing.JButton saveBtn;
    private javax.swing.JComboBox<String> statusComboBox;
    // End of variables declaration//GEN-END:variables
}

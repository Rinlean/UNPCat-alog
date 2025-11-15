package main;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import main.stuff.dbconn;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.sql.*;

public class editCatMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(editCatMenu.class.getName());

    private final Integer accountId;
    private final catProfileMenu parentProfile;

    public editCatMenu(int accountId, catProfileMenu catProf) {
        this.accountId = accountId;
        this.parentProfile = catProf;
        initComponents();
        loadAreas();
        loadCats();
        SwingUtilities.invokeLater(this::positionNextToParent);
    }

    private void positionNextToParent() {
        try {
            if (parentProfile != null && parentProfile.isDisplayable() && parentProfile.isVisible()) {
                Point p;
                try {
                    p = parentProfile.getLocationOnScreen();
                } catch (IllegalComponentStateException e) {
                    setLocationRelativeTo(null);
                    return;
                }
                Dimension pSize = parentProfile.getSize();
                Dimension mySize = this.getSize();
                int margin = 10;

                Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

                int x = p.x + pSize.width + margin; // try to the right
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

            // If accountId == 0 (admin) show all cats, otherwise show only cats tied to this caretaker
            boolean filterByCaretaker = (accountId != null && accountId != 0);
            String sql;
            if (filterByCaretaker) {
                // join to association table cat_caretaker
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

    private void onCatSelected(ActionEvent evt) {
        Object selObj = catSelector.getSelectedItem();
        CatItem item = (selObj instanceof CatItem) ? (CatItem) selObj : null;

        if (item == null) {
            return;
        }
        if (item.id == 0) {
            nameField.setText("");
            genderCombo.setSelectedItem("Unknown");
            breedField.setText("");
            colorField.setText("");
            areaCombo.setSelectedIndex(0);
            updateParentProfile(0);
            // clear comment areas
            HealthTextArea.setText("");
            IncidentsTextArea.setText("");
        } else {
            loadCatDetails(item.id);
            updateParentProfile(item.id);
        }
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
        if (parentProfile == null) {
            return;
        }

        String[] candidateNames = {"setCatId", "loadCat", "refresh", "reload", "setId", "showCat"};

        Class<?> cls = parentProfile.getClass();
        for (String name : candidateNames) {
            try {
                Method m = cls.getMethod(name, int.class);
                try {
                    m.invoke(parentProfile, catId);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            parentProfile.repaint();
                        } catch (Throwable ignored) {
                        }
                    });
                    return;
                } catch (Throwable invokeErr) {
                    logger.log(java.util.logging.Level.FINE, "Invocation of " + name + " failed", invokeErr);
                }
            } catch (NoSuchMethodException ignored) {
                try {
                    Method m2 = cls.getMethod(name, Integer.class);
                    try {
                        m2.invoke(parentProfile, Integer.valueOf(catId));
                        SwingUtilities.invokeLater(parentProfile::repaint);
                        return;
                    } catch (Throwable invokeErr) {
                        logger.log(java.util.logging.Level.FINE, "Invocation of " + name + "(Integer) failed", invokeErr);
                    }
                } catch (NoSuchMethodException ignored2) {
                    try {
                        Method m3 = cls.getMethod(name, String.class);
                        try {
                            m3.invoke(parentProfile, String.valueOf(catId));
                            SwingUtilities.invokeLater(parentProfile::repaint);
                            return;
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

        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    parentProfile.dispose();
                } catch (Throwable ignored) {
                }
                if (catId >= 0) {
                    try {
                        catProfileMenu newProf = new catProfileMenu(catId);
                        newProf.setVisible(true);
                    } catch (Throwable t) {
                        logger.log(java.util.logging.Level.FINE, "Failed to open fallback profile", t);
                    }
                }
            });
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.FINE, "Failed to fallback-refresh parent profile", t);
        }
    }

    private void refreshHealthComments(int catId) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            String sql = "SELECT date, conditions FROM health_record WHERE cat_id = ? ORDER BY date DESC";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, catId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        Date d = rs.getDate("date");
                        String cond = rs.getString("conditions");
                        if (!first) {
                            sb.append("\n\n");
                        }
                        sb.append((d == null) ? "Unknown date" : d.toString());
                        sb.append(" - ");
                        sb.append(cond == null ? "" : cond);
                        first = false;
                    }
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.FINE, "Failed to load health comments", ex);
                sb.append("Failed to load health comments: ").append(ex.getMessage());
            }
            final String out = sb.toString();
            SwingUtilities.invokeLater(() -> HealthTextArea.setText(out));
        });
    }

    private void refreshIncidentComments(int catId) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            String sql = "SELECT date, `desc` FROM incident_report WHERE cat_id = ? ORDER BY date DESC";
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, catId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("date");
                        String desc = rs.getString("desc");
                        if (!first) {
                            sb.append("\n\n");
                        }
                        sb.append((ts == null) ? "Unknown time" : ts.toString());
                        sb.append(" - ");
                        sb.append(desc == null ? "" : desc);
                        first = false;
                    }
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.FINE, "Failed to load incident comments", ex);
                sb.append("Failed to load incident comments: ").append(ex.getMessage());
            }
            final String out = sb.toString();
            SwingUtilities.invokeLater(() -> IncidentsTextArea.setText(out));
        });
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        genderCombo = new javax.swing.JComboBox<>();
        catSelector = new javax.swing.JComboBox<>();
        areaCombo = new javax.swing.JComboBox<>();
        nameField = new javax.swing.JTextField();
        breedField = new javax.swing.JTextField();
        colorField = new javax.swing.JTextField();
        saveBtn = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        HealthTextArea = new javax.swing.JTextArea();
        AddHealthCommentBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        IncidentsTextArea = new javax.swing.JTextArea();
        AddIncidentCommentBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Cat Menu");
        setMaximumSize(new java.awt.Dimension(400, 723));
        setPreferredSize(new java.awt.Dimension(400, 723));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        genderCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Male", "Female", "Unknown"}));
        genderCombo.setBorder(javax.swing.BorderFactory.createTitledBorder("Gender"));
        getContentPane().add(genderCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 30, 130, -1));

        catSelector.setModel(new javax.swing.DefaultComboBoxModel());
        catSelector.setBorder(javax.swing.BorderFactory.createTitledBorder("Cats"));
        catSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                catSelectorActionPerformed(evt);
            }
        });
        getContentPane().add(catSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 30, 190, -1));

        areaCombo.setModel(new javax.swing.DefaultComboBoxModel<>());
        areaCombo.setBorder(javax.swing.BorderFactory.createTitledBorder("Area"));
        getContentPane().add(areaCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 270, 340, -1));

        nameField.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        getContentPane().add(nameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 340, -1));

        breedField.setBorder(javax.swing.BorderFactory.createTitledBorder("Breed"));
        getContentPane().add(breedField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, 340, -1));

        colorField.setBorder(javax.swing.BorderFactory.createTitledBorder("Color"));
        getContentPane().add(colorField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, 340, -1));

        saveBtn.setText("Save");
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBtnActionPerformed(evt);
            }
        });
        getContentPane().add(saveBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 330, -1, -1));

        HealthTextArea.setColumns(20);
        HealthTextArea.setRows(5);
        jScrollPane1.setViewportView(HealthTextArea);

        AddHealthCommentBtn.setText("Comment");
        AddHealthCommentBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddHealthCommentBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(AddHealthCommentBtn)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 366, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 28, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AddHealthCommentBtn)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Health", jPanel1);

        IncidentsTextArea.setColumns(20);
        IncidentsTextArea.setRows(5);
        jScrollPane2.setViewportView(IncidentsTextArea);

        AddIncidentCommentBtn.setText("Comment");
        AddIncidentCommentBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddIncidentCommentBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(AddIncidentCommentBtn)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 363, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 31, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AddIncidentCommentBtn)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Incidents", jPanel2);

        getContentPane().add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 360, 400, 360));

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
            // insert new cat (no caretaker_id column in cat table)
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

                // If opened by a caretaker (non-admin), create association in cat_caretaker
                if (accountId != null && accountId != 0 && newId > 0) {
                    String assocSql = "INSERT INTO cat_caretaker (cat_id, caretaker_id) VALUES (?, ?)";
                    try (PreparedStatement ps2 = conn.prepareStatement(assocSql)) {
                        ps2.setInt(1, newId);
                        ps2.setInt(2, accountId);
                        ps2.executeUpdate();
                    } catch (SQLException assocEx) {
                        // log but don't fail the whole operation; association can be retried/managed later
                        logger.log(java.util.logging.Level.WARNING, "Failed to create cat_caretaker association", assocEx);
                    }
                }

                JOptionPane.showMessageDialog(this, "Cat added (id=" + newId + ").", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadCats();
                if (newId > 0) {
                    selectCatById(newId);
                }
                updateParentProfile(newId);
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to insert cat", ex);
                JOptionPane.showMessageDialog(this, "Failed to insert cat: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // update existing cat (do not attempt to modify caretakers here)
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
        onCatSelected(evt);
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

        // Insert into DB
        String insertSql = "INSERT INTO health_record (cat_id, conditions, date) VALUES (?, ?, CURRENT_DATE())";
        try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, sel.id);
            ps.setString(2, comment);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                JOptionPane.showMessageDialog(this, "Failed to add health comment.", "DB error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Health comment added.", "Success", JOptionPane.INFORMATION_MESSAGE);
                // Clear the text area (we do NOT show past comments in the text area)
                HealthTextArea.setText("");
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
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to insert incident report", ex);
            JOptionPane.showMessageDialog(this, "Failed to add incident report: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_AddIncidentCommentBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddHealthCommentBtn;
    private javax.swing.JButton AddIncidentCommentBtn;
    private javax.swing.JTextArea HealthTextArea;
    private javax.swing.JTextArea IncidentsTextArea;
    private javax.swing.JComboBox<String> areaCombo;
    private javax.swing.JTextField breedField;
    private javax.swing.JComboBox<String> catSelector;
    private javax.swing.JTextField colorField;
    private javax.swing.JComboBox<String> genderCombo;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField nameField;
    private javax.swing.JButton saveBtn;
    // End of variables declaration//GEN-END:variables
}

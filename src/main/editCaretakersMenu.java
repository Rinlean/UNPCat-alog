package main;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class editCaretakersMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(editCaretakersMenu.class.getName());
    private Integer selectedCaretakerId = null;

    public editCaretakersMenu() {
        initComponents();
        loadCaretakers();

        listCaretakers.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                Object sel = listCaretakers.getSelectedValue();
                CaretakerItem ct = (sel instanceof CaretakerItem) ? (CaretakerItem) sel : null;
                onCaretakerSelected(ct);
            }
        });
        setDetailsEnabled(false);
    }

    private void setDetailsEnabled(boolean enabled) {
        changeDetailsPanel.setEnabled(enabled);
        nameField.setEnabled(enabled);
        contactField.setEnabled(enabled);
        userField.setEnabled(enabled);
        passField.setEnabled(enabled);
        SaveBtn.setEnabled(enabled);
        DeleteBtn.setEnabled(enabled);
    }

    private void loadCaretakers() {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<CaretakerItem> model = new DefaultListModel<>();
            String sql = "SELECT caretaker_id, name, contact_info FROM caretaker ORDER BY name";
            try (Connection conn = main.stuff.dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addElement(new CaretakerItem(rs.getInt("caretaker_id"),
                            rs.getString("name"),
                            rs.getString("contact_info")));
                }
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.WARNING, "Failed to load caretakers", ex);
                model.clear();
                model.addElement(new CaretakerItem(0, "<Error loading caretakers>", ""));
            }


            SwingUtilities.invokeLater(() -> {
                @SuppressWarnings("unchecked")
                javax.swing.JList<CaretakerItem> lst = (javax.swing.JList<CaretakerItem>) (Object) listCaretakers;
                lst.setModel(model);
                lst.clearSelection();
            });
        });
    }

    private void onCaretakerSelected(CaretakerItem ct) {
        if (ct == null) {
            selectedCaretakerId = null;
            nameLabel.setText("");
            contactLabel.setText("");
            usernameLabel.setText("");
            nameField.setText("");
            contactField.setText("");
            userField.setText("");
            passField.setText("");
            setDetailsEnabled(false);
            return;
        }

        selectedCaretakerId = ct.id;
        nameLabel.setText(ct.name);
        contactLabel.setText(ct.contact == null ? "" : ct.contact);
        nameField.setText(ct.name);
        contactField.setText(ct.contact == null ? "" : ct.contact);
        userField.setText("");
        passField.setText("");

        String username = "";
        String accountsSql = "SELECT username FROM accounts WHERE caretaker_id = ? LIMIT 1";
        try (Connection conn = main.stuff.dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(accountsSql)) {
            ps.setInt(1, ct.id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                    if (username == null) {
                        username = "";
                    }
                }
            }
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.FINE, "Failed to load account username for caretaker " + ct.id, ex);
        }
        usernameLabel.setText(username);
        setDetailsEnabled(true);
    }

    private void selectCaretakerInListById(int caretakerId) {
        if (caretakerId <= 0) {
            return;
        }
        @SuppressWarnings("unchecked")
        javax.swing.JList<CaretakerItem> lst = (javax.swing.JList<CaretakerItem>) (Object) listCaretakers;
        javax.swing.ListModel<CaretakerItem> model = lst.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            CaretakerItem ci = model.getElementAt(i);
            if (ci != null && ci.id == caretakerId) {
                final int idx = i;
                SwingUtilities.invokeLater(() -> {
                    lst.setSelectedIndex(idx);
                    lst.ensureIndexIsVisible(idx);
                });
                return;
            }
        }
    }

    private static final class CaretakerItem {

        final int id;
        final String name;
        final String contact;

        CaretakerItem(int id, String name, String contact) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.contact = contact == null ? "" : contact;
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

        SaveBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        listCaretakers = new javax.swing.JList<>();
        nameLabel = new javax.swing.JLabel();
        contactLabel = new javax.swing.JLabel();
        changeDetailsPanel = new javax.swing.JPanel();
        nameField = new javax.swing.JTextField();
        contactField = new javax.swing.JTextField();
        userField = new javax.swing.JTextField();
        passField = new javax.swing.JPasswordField();
        DeleteBtn = new javax.swing.JButton();
        usernameLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Caretakers Menu");
        setMinimumSize(new java.awt.Dimension(422, 350));
        setPreferredSize(new java.awt.Dimension(432, 360));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        SaveBtn.setText("Save");
        SaveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveBtnActionPerformed(evt);
            }
        });
        getContentPane().add(SaveBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 290, -1, -1));

        listCaretakers.setBorder(javax.swing.BorderFactory.createTitledBorder("List of Caretakers"));
        listCaretakers.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(listCaretakers);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(23, 20, 135, 124));

        nameLabel.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        getContentPane().add(nameLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(172, 20, 220, -1));

        contactLabel.setBorder(javax.swing.BorderFactory.createTitledBorder("Contact"));
        getContentPane().add(contactLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(172, 65, 220, -1));

        changeDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Change Caretaker Details"));
        changeDetailsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        nameField.setText("jTextField1");
        nameField.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        changeDetailsPanel.add(nameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, 188, -1));

        contactField.setText("jTextField2");
        contactField.setBorder(javax.swing.BorderFactory.createTitledBorder("Contact"));
        changeDetailsPanel.add(contactField, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, 188, -1));

        userField.setText("jTextField3");
        userField.setBorder(javax.swing.BorderFactory.createTitledBorder("Username"));
        changeDetailsPanel.add(userField, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 20, 170, -1));

        passField.setText("jPasswordField1");
        passField.setBorder(javax.swing.BorderFactory.createTitledBorder("Password"));
        changeDetailsPanel.add(passField, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 80, 170, -1));

        getContentPane().add(changeDetailsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 150, 390, 130));

        DeleteBtn.setText("Delete");
        DeleteBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteBtnActionPerformed(evt);
            }
        });
        getContentPane().add(DeleteBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 290, -1, -1));

        usernameLabel.setBorder(javax.swing.BorderFactory.createTitledBorder("Username"));
        getContentPane().add(usernameLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(172, 110, 220, -1));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void SaveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveBtnActionPerformed
        if (selectedCaretakerId == null || selectedCaretakerId == 0) {
            JOptionPane.showMessageDialog(this, "Please select a caretaker to save changes.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newName = nameField.getText() == null ? "" : nameField.getText().trim();
        String newContact = contactField.getText() == null ? "" : contactField.getText().trim();
        String newUsername = userField.getText() == null ? "" : userField.getText().trim();
        String newPassword = new String(passField.getPassword()).trim();

        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Caretaker name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String updateCtSql = "UPDATE caretaker SET name = ?, contact_info = ? WHERE caretaker_id = ?";
        String upsertAccountSql = "SELECT account_id FROM accounts WHERE caretaker_id = ? LIMIT 1";
        String insertAccountSql = "INSERT INTO accounts (username, password, account_type, caretaker_id) VALUES (?, ?, 'caretaker', ?)";
        String updateAccountSql = "UPDATE accounts SET username = ?, password = ? WHERE caretaker_id = ?";

        try (Connection conn = main.stuff.dbconn.getConnection()) {
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(updateCtSql)) {
                    ps.setString(1, newName);
                    ps.setString(2, newContact.isEmpty() ? null : newContact);
                    ps.setInt(3, selectedCaretakerId);
                    ps.executeUpdate();
                }

                if (!newUsername.isEmpty() || !newPassword.isEmpty()) {
                    boolean accountExists = false;
                    try (PreparedStatement ps = conn.prepareStatement(upsertAccountSql)) {
                        ps.setInt(1, selectedCaretakerId);
                        try (ResultSet rs = ps.executeQuery()) {
                            accountExists = rs.next();
                        }
                    }

                    if (accountExists) {
                        String existingUsername = null;
                        try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM accounts WHERE caretaker_id = ? LIMIT 1")) {
                            ps.setInt(1, selectedCaretakerId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    existingUsername = rs.getString("username");
                                }
                            }
                        }
                        String usernameToSet = newUsername.isEmpty() ? existingUsername : newUsername;
                        String passwordToSet = newPassword.isEmpty() ? null : newPassword;

                        try (PreparedStatement ps = conn.prepareStatement(updateAccountSql)) {
                            ps.setString(1, usernameToSet);
                            ps.setString(2, passwordToSet);
                            ps.setInt(3, selectedCaretakerId);
                            ps.executeUpdate();
                        }
                    } else {
                        if (!newUsername.isEmpty()) {
                            try (PreparedStatement ps = conn.prepareStatement(insertAccountSql)) {
                                ps.setString(1, newUsername);
                                ps.setString(2, newPassword.isEmpty() ? null : newPassword);
                                ps.setInt(3, selectedCaretakerId);
                                ps.executeUpdate();
                            }
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
            logger.log(java.util.logging.Level.SEVERE, "Failed to save caretaker changes", ex);
            JOptionPane.showMessageDialog(this, "Failed to save changes: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Caretaker updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
        loadCaretakers();
        SwingUtilities.invokeLater(() -> selectCaretakerInListById(selectedCaretakerId));
    }//GEN-LAST:event_SaveBtnActionPerformed

    private void DeleteBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteBtnActionPerformed
        if (selectedCaretakerId == null || selectedCaretakerId == 0) {
            JOptionPane.showMessageDialog(this, "Please select a caretaker to delete.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete caretaker and associated account/associations? This cannot be undone.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String deleteAccountSql = "DELETE FROM accounts WHERE caretaker_id = ?";
        String deleteCaretakerSql = "DELETE FROM caretaker WHERE caretaker_id = ?";

        try (Connection conn = main.stuff.dbconn.getConnection()) {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(deleteAccountSql)) {
                    ps.setInt(1, selectedCaretakerId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement(deleteCaretakerSql)) {
                    ps2.setInt(1, selectedCaretakerId);
                    int affected = ps2.executeUpdate();
                    if (affected == 0) {
                        throw new SQLException("No caretaker removed (it may have already been deleted).");
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
            logger.log(java.util.logging.Level.SEVERE, "Failed to delete caretaker", ex);
            JOptionPane.showMessageDialog(this, "Failed to delete caretaker: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Caretaker deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
        loadCaretakers();
        SwingUtilities.invokeLater(() -> onCaretakerSelected(null));
    }//GEN-LAST:event_DeleteBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton DeleteBtn;
    private javax.swing.JButton SaveBtn;
    private javax.swing.JPanel changeDetailsPanel;
    private javax.swing.JTextField contactField;
    private javax.swing.JLabel contactLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<String> listCaretakers;
    private javax.swing.JTextField nameField;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JPasswordField passField;
    private javax.swing.JTextField userField;
    private javax.swing.JLabel usernameLabel;
    // End of variables declaration//GEN-END:variables
}

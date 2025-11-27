package main;

import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import main.stuff.dbconn;

public class ProfileMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ProfileMenu.class.getName());

    private final int accountId;

    public ProfileMenu(int accountId) {
        this.accountId = accountId;
        initComponents();
        if (accountId > 0) {
            fetchAndPopulate();
        } else {
            setTitle("Profile");
        }
    }

    private Connection getConnection() throws Exception {
        return dbconn.getConnection();
    }

    private void fetchAndPopulate() {
        new SwingWorker<Void, Void>() {
            Exception error;
            String username = "";
            String caretakerName = "";
            String caretakerContact = "";

            @Override
            protected Void doInBackground() {
                final String accSql = "SELECT username, caretaker_id FROM accounts WHERE account_id = ?";
                final String ctSql = "SELECT name, contact_info FROM caretaker WHERE caretaker_id = ?";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(accSql)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("No account found with id " + accountId);
                        }
                        username = rs.getString("username");
                        int caretakerId = rs.getInt("caretaker_id");
                        if (!rs.wasNull() && caretakerId > 0) {
                            try (PreparedStatement ps2 = conn.prepareStatement(ctSql)) {
                                ps2.setInt(1, caretakerId);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (rs2.next()) {
                                        caretakerName = rs2.getString("name");
                                        caretakerContact = rs2.getString("contact_info");
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    logger.log(java.util.logging.Level.SEVERE, "Failed to load profile", ex);
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ProfileMenu.this,
                                "Error loading profile:\n" + error.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        setID.setText("");
                        setName.setText("");
                        setContact.setText("");
                        setTitle("Profile");
                    });
                    return;
                }

                final String displayName = (caretakerName != null && !caretakerName.isEmpty()) ? caretakerName
                        : (username != null ? username : "");
                final String displayContact = (caretakerContact == null) ? "" : caretakerContact;
                SwingUtilities.invokeLater(() -> {
                    setID.setText(String.valueOf(accountId));
                    setUsername.setText(username);
                    setName.setText(displayName);
                    setContact.setText(displayContact);
                    setTitle("Profile: " + (displayName == null ? "" : displayName));
                });
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setID = new javax.swing.JLabel();
        setName = new javax.swing.JLabel();
        setContact = new javax.swing.JLabel();
        OldPassword = new javax.swing.JPasswordField();
        NewPassword = new javax.swing.JPasswordField();
        ConfirmPassword = new javax.swing.JPasswordField();
        LogoutBtn = new javax.swing.JButton();
        SaveBtn = new javax.swing.JButton();
        setUsername = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(400, 350));
        setMinimumSize(new java.awt.Dimension(400, 350));
        setPreferredSize(new java.awt.Dimension(400, 370));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setID.setText("jLabel1");
        setID.setBorder(javax.swing.BorderFactory.createTitledBorder("ID"));
        getContentPane().add(setID, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 20, 70, -1));

        setName.setText("jLabel2");
        setName.setBorder(javax.swing.BorderFactory.createTitledBorder("Name"));
        getContentPane().add(setName, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 70, 200, -1));

        setContact.setText("jLabel3");
        setContact.setBorder(javax.swing.BorderFactory.createTitledBorder("Contact"));
        getContentPane().add(setContact, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 110, 200, -1));

        OldPassword.setBorder(javax.swing.BorderFactory.createTitledBorder("Old Password"));
        getContentPane().add(OldPassword, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 170, 200, -1));

        NewPassword.setBorder(javax.swing.BorderFactory.createTitledBorder("New Password"));
        getContentPane().add(NewPassword, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 220, 200, -1));

        ConfirmPassword.setBorder(javax.swing.BorderFactory.createTitledBorder("Confirm Password"));
        getContentPane().add(ConfirmPassword, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 270, 200, -1));

        LogoutBtn.setText("Logout");
        LogoutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LogoutBtnActionPerformed(evt);
            }
        });
        getContentPane().add(LogoutBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 280, -1, -1));

        SaveBtn.setText("Save");
        SaveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveBtnActionPerformed(evt);
            }
        });
        getContentPane().add(SaveBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 240, -1, -1));

        setUsername.setText("jLabel1");
        setUsername.setBorder(javax.swing.BorderFactory.createTitledBorder("Username"));
        getContentPane().add(setUsername, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 20, 120, -1));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void LogoutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LogoutBtnActionPerformed
        final String userIdStr = "";
        final String acctType = "";
        boolean appliedToExisting = false;

        for (Window w : Window.getWindows()) {
            if (w instanceof qrMenu) {
                qrMenu existing = (qrMenu) w;
                existing.applyLogin(userIdStr, acctType);
                appliedToExisting = true;
                break;
            }
        }

        if (!appliedToExisting) {
            qrMenu qr = new qrMenu(userIdStr, acctType);
            qr.setVisible(true);
        }

        this.dispose();

    }//GEN-LAST:event_LogoutBtnActionPerformed

    private void SaveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveBtnActionPerformed

        final char[] oldPwdChars = OldPassword.getPassword();
        final char[] newPwdChars = NewPassword.getPassword();
        final char[] confirmPwdChars = ConfirmPassword.getPassword();

        if (accountId <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid account. Cannot change password.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(oldPwdChars, '\0');
            Arrays.fill(newPwdChars, '\0');
            Arrays.fill(confirmPwdChars, '\0');
            return;
        }

        if (oldPwdChars == null || oldPwdChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter your old password.", "Validation", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(oldPwdChars, '\0');
            Arrays.fill(newPwdChars, '\0');
            Arrays.fill(confirmPwdChars, '\0');
            return;
        }

        if (newPwdChars == null || newPwdChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter a new password.", "Validation", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(oldPwdChars, '\0');
            Arrays.fill(newPwdChars, '\0');
            Arrays.fill(confirmPwdChars, '\0');
            return;
        }

        if (!Arrays.equals(newPwdChars, confirmPwdChars)) {
            JOptionPane.showMessageDialog(this, "New password and confirmation do not match.", "Validation", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(oldPwdChars, '\0');
            Arrays.fill(newPwdChars, '\0');
            Arrays.fill(confirmPwdChars, '\0');
            return;
        }

        final String oldPwd = new String(oldPwdChars);
        final String newPwd = new String(newPwdChars);
        Arrays.fill(oldPwdChars, '\0');
        Arrays.fill(newPwdChars, '\0');
        Arrays.fill(confirmPwdChars, '\0');

        new SwingWorker<Void, Void>() {
            Exception error;
            boolean success = false;
            String errorMessage = null;

            @Override
            protected Void doInBackground() {
                final String selectSql = "SELECT password FROM accounts WHERE account_id = ?";
                final String updateSql = "UPDATE accounts SET password = ? WHERE account_id = ?";
                try (Connection conn = getConnection()) {
                    String currentStored = null;
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setInt(1, accountId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                currentStored = rs.getString("password");
                            } else {
                                errorMessage = "Account not found.";
                                return null;
                            }
                        }
                    }

                    if (currentStored == null) {
                        currentStored = "";
                    }

                    if (!currentStored.equals(oldPwd)) {
                        errorMessage = "Old password is incorrect.";
                        return null;
                    }

                    try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                        ps2.setString(1, newPwd);
                        ps2.setInt(2, accountId);
                        int updated = ps2.executeUpdate();
                        if (updated == 1) {
                            success = true;
                        } else {
                            errorMessage = "Failed to update password.";
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    logger.log(java.util.logging.Level.SEVERE, "Failed to change password", ex);
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    final String msg = "An error occurred while changing password:\n" + error.getMessage();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ProfileMenu.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                        // clear fields in UI
                        OldPassword.setText("");
                        NewPassword.setText("");
                        ConfirmPassword.setText("");
                    });
                    return;
                }

                if (!success) {
                    final String msg = (errorMessage != null) ? errorMessage : "Unknown error changing password.";
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ProfileMenu.this, msg, "Change Password", JOptionPane.WARNING_MESSAGE);
                        OldPassword.setText("");
                        NewPassword.setText("");
                        ConfirmPassword.setText("");
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(ProfileMenu.this, "Password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    OldPassword.setText("");
                    NewPassword.setText("");
                    ConfirmPassword.setText("");
                });
            }
        }.execute();
    }//GEN-LAST:event_SaveBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPasswordField ConfirmPassword;
    private javax.swing.JButton LogoutBtn;
    private javax.swing.JPasswordField NewPassword;
    private javax.swing.JPasswordField OldPassword;
    private javax.swing.JButton SaveBtn;
    private javax.swing.JLabel setContact;
    private javax.swing.JLabel setID;
    private javax.swing.JLabel setName;
    private javax.swing.JLabel setUsername;
    // End of variables declaration//GEN-END:variables
}

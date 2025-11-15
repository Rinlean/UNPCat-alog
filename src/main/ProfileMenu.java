package main;

import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                    setName.setText(displayName);
                    setContact.setText(displayContact);
                    setTitle("Profile: " + (username == null ? "" : username));
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
        nameLabel = new javax.swing.JLabel();
        IDlabel = new javax.swing.JLabel();
        Contact = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jPasswordField2 = new javax.swing.JPasswordField();
        jPasswordField3 = new javax.swing.JPasswordField();
        LogoutBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(400, 350));
        setMinimumSize(new java.awt.Dimension(400, 350));
        setPreferredSize(new java.awt.Dimension(400, 350));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setID.setText("jLabel1");
        getContentPane().add(setID, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 20, -1, -1));

        setName.setText("jLabel2");
        getContentPane().add(setName, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 70, -1, -1));

        setContact.setText("jLabel3");
        getContentPane().add(setContact, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 130, -1, -1));

        nameLabel.setText("Name");
        getContentPane().add(nameLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 70, -1, -1));

        IDlabel.setText("ID");
        getContentPane().add(IDlabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 20, -1, -1));

        Contact.setText("Contact");
        getContentPane().add(Contact, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 130, -1, -1));

        jPasswordField1.setBorder(javax.swing.BorderFactory.createTitledBorder("Password"));
        jPasswordField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPasswordField1ActionPerformed(evt);
            }
        });
        getContentPane().add(jPasswordField1, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 170, 153, -1));

        jPasswordField2.setBorder(javax.swing.BorderFactory.createTitledBorder("Password"));
        jPasswordField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPasswordField2ActionPerformed(evt);
            }
        });
        getContentPane().add(jPasswordField2, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 220, 153, -1));

        jPasswordField3.setBorder(javax.swing.BorderFactory.createTitledBorder("Password"));
        getContentPane().add(jPasswordField3, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 270, 153, -1));

        LogoutBtn.setText("Logout");
        LogoutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LogoutBtnActionPerformed(evt);
            }
        });
        getContentPane().add(LogoutBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 280, -1, -1));

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

    private void jPasswordField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPasswordField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jPasswordField2ActionPerformed

    private void jPasswordField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPasswordField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jPasswordField1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Contact;
    private javax.swing.JLabel IDlabel;
    private javax.swing.JButton LogoutBtn;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JPasswordField jPasswordField3;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel setContact;
    private javax.swing.JLabel setID;
    private javax.swing.JLabel setName;
    // End of variables declaration//GEN-END:variables
}

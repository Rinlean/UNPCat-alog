package main;

import java.sql.PreparedStatement;
import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import main.stuff.dbconn;

public class loginMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loginMenu.class.getName());

    public loginMenu() {
        initComponents();
        txtUsername.addActionListener(e -> pfieldPassword.requestFocusInWindow());
        pfieldPassword.addActionListener(e -> loginBtn.doClick());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pfieldPassword = new javax.swing.JPasswordField();
        txtUsername = new javax.swing.JTextField();
        cancelBtn = new javax.swing.JButton();
        loginBtn = new javax.swing.JButton();
        LoginLabel = new javax.swing.JLabel();
        registerBtn = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setMaximumSize(new java.awt.Dimension(300, 320));
        setMinimumSize(new java.awt.Dimension(300, 320));
        setPreferredSize(new java.awt.Dimension(300, 320));
        setResizable(false);

        pfieldPassword.setBorder(javax.swing.BorderFactory.createTitledBorder("Password"));

        txtUsername.setBorder(javax.swing.BorderFactory.createTitledBorder("Username"));
        txtUsername.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtUsernameActionPerformed(evt);
            }
        });

        cancelBtn.setText("Cancel");
        cancelBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });

        loginBtn.setText("Login");
        loginBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        loginBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginBtnActionPerformed(evt);
            }
        });

        LoginLabel.setFont(new java.awt.Font("Arial Black", 1, 36)); // NOI18N
        LoginLabel.setText("Login");

        registerBtn.setForeground(new java.awt.Color(0, 255, 0));
        registerBtn.setText("Register");
        registerBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        registerBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                registerBtnMouseClicked(evt);
            }
        });

        jLabel5.setText("Dont have an account?");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(registerBtn))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(cancelBtn)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(loginBtn))
                                .addComponent(pfieldPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                                .addComponent(txtUsername))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(89, 89, 89)
                        .addComponent(LoginLabel)))
                .addGap(59, 59, 59))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(LoginLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(pfieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(loginBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(registerBtn)
                    .addComponent(jLabel5))
                .addContainerGap())
        );

        txtUsername.getAccessibleContext().setAccessibleName("");
        txtUsername.getAccessibleContext().setAccessibleDescription("");

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelBtnActionPerformed

    private void loginBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginBtnActionPerformed
        final String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        final char[] passwordChars = pfieldPassword.getPassword();

        // Basic validation
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username.", "Missing username", JOptionPane.WARNING_MESSAGE);
            // Clear password from memory
            Arrays.fill(passwordChars, '\0');
            return;
        }
        if (passwordChars == null || passwordChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter a password.", "Missing password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert to String only when necessary and clear the char[] afterwards
        final String password = new String(passwordChars);

        final String sql = "SELECT account_id, account_type, password FROM accounts WHERE username = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Username not found. Please check your username or register.", "Login failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                final int userid = rs.getInt("account_id");
                String acctType = rs.getString("account_type");
                if (acctType == null) {
                    acctType = "";
                }

                final String storedPassword = rs.getString("password");
                boolean passwordMatches = storedPassword != null && storedPassword.equals(password);

                if (passwordMatches) {
                    final String userIdStr = Integer.toString(userid);
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
                } else {
                    // Password mismatch
                    JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Login failed", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (ClassNotFoundException e) {
            // Driver not found
            JOptionPane.showMessageDialog(this, "Database driver not found: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            // DB error
            JOptionPane.showMessageDialog(this, "Database access error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            // Unexpected error
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Clear sensitive data
            Arrays.fill(passwordChars, '\0');
        }
    }//GEN-LAST:event_loginBtnActionPerformed

    private void txtUsernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtUsernameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtUsernameActionPerformed

    private void registerBtnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_registerBtnMouseClicked
        signupMenu sMenu = new signupMenu();
        sMenu.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_registerBtnMouseClicked

    private Connection getConnection() throws Exception {
        return dbconn.getConnection();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LoginLabel;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JButton loginBtn;
    private javax.swing.JPasswordField pfieldPassword;
    private javax.swing.JLabel registerBtn;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}

package main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import javax.swing.SwingWorker;
import main.stuff.QRstuff;
import main.stuff.dbconn;

public class qrMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(qrMenu.class.getName());

    private QRstuff qrStuff;
    private boolean darkMode = false;
    private Integer accountId = null;

    private enum Role {
        ADMIN, CARETAKER, GUEST
    }
    private Role accountRole = Role.GUEST;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-background");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean windowListenerAdded = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);

    public qrMenu(String accId, String accType) {
        initComponents();
        applyLogin(accId, accType);
    }

    public final void applyLogin(String accIdStr, String accTypeStr) {
        Integer accId = null;
        if (accIdStr != null && !accIdStr.isEmpty()) {
            try {
                accId = Integer.parseInt(accIdStr);
            } catch (NumberFormatException ex) {
                logger.log(Level.FINE, "Invalid accId string: {0}", accIdStr);
            }
        }

        Role role = Role.GUEST;
        if ("admin".equalsIgnoreCase(accTypeStr)) {
            role = Role.ADMIN;
        } else if ("caretaker".equalsIgnoreCase(accTypeStr)) {
            role = Role.CARETAKER;
        }

        this.accountId = accId;
        this.accountRole = role;

        // Update UI elements
        SwingUtilities.invokeLater(() -> updateMenusForRole());

        if (this.accountId != null) {
            backgroundExecutor.submit(() -> {
                String title = "UNP Cat-alog";
                final String sql
                        = "SELECT c.name AS caretaker_name, a.username "
                        + "FROM accounts a "
                        + "LEFT JOIN caretaker c ON a.caretaker_id = c.caretaker_id "
                        + "WHERE a.account_id = ?";
                try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, this.accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String caretakerName = rs.getString("caretaker_name");
                            String username = rs.getString("username");
                            if (caretakerName != null && !caretakerName.trim().isEmpty()) {
                                title = "UNP Cat-alog - User: " + caretakerName;
                            } else if (username != null && !username.trim().isEmpty()) {
                                title = "UNP Cat-alog - User: " + username;
                            }
                        }
                    }
                } catch (SQLException ex) {
                    logger.log(Level.FINE, "Failed to load account/caretaker name", ex);
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Unexpected error loading name", ex);
                } finally {
                    final String finalTitle = title;
                    SwingUtilities.invokeLater(() -> setTitle(finalTitle));
                }
            });
        } else {
            SwingUtilities.invokeLater(() -> setTitle("UNP Cat-alog"));
        }
    }

    private void startQRPrev() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
        });

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                qrStuff = new QRstuff();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    SwingUtilities.invokeLater(() -> {
                        try {
                            qrStuff.attachToPanel(webcamPanel);
                        } catch (Throwable t) {
                            logger.log(Level.SEVERE, "Failed to attach preview panel", t);
                            JOptionPane.showMessageDialog(qrMenu.this, "Failed to attach preview: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });

                    backgroundExecutor.submit(() -> {
                        try {
                            qrStuff.startScanning();
                        } catch (Throwable t) {
                            logger.log(Level.FINE, "qrStuff.startScanning threw", t);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(qrMenu.this, "Failed to start QR scanning:\n" + t.getMessage(), "Scanner error", JOptionPane.ERROR_MESSAGE);
                            });
                            started.set(false);
                            SwingUtilities.invokeLater(() -> {
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                            });
                        }
                    });

                    // Update UI buttons to running state
                    SwingUtilities.invokeLater(() -> {
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                    });

                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Failed to initialize QR preview", ex);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(qrMenu.this, "Failed to initialize camera:\n" + ex.getMessage(), "Camera error", JOptionPane.ERROR_MESSAGE);
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    });
                    if (qrStuff != null) {
                        qrStuff.submitCleanup(() -> {
                            SwingUtilities.invokeLater(() -> {
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                started.set(false);
                            });
                        });
                    }
                }
            }
        }.execute();

        // Ensure QRStuff is disposed when the window closes
        if (windowListenerAdded.compareAndSet(false, true)) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            webcamPanel.removeAll();
                            webcamPanel.revalidate();
                            webcamPanel.repaint();
                        } catch (Throwable ignored) {
                        }
                    });

                    if (qrStuff != null) {
                        qrStuff.submitCleanup(() -> {
                        });
                    } else {
                    }
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    if (qrStuff != null) {
                        qrStuff.submitCleanup(() -> {
                            SwingUtilities.invokeLater(() -> {
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                started.set(false);
                            });
                        });
                    }
                    // shutdown background executor used for non-QR tasks
                    backgroundExecutor.shutdown();
                    try {
                        if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                            backgroundExecutor.shutdownNow();
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        backgroundExecutor.shutdownNow();
                    }
                }
            });
        }
    }

    private void submitCleanup() {
        if (qrStuff != null) {
            qrStuff.submitCleanup(() -> {
                SwingUtilities.invokeLater(() -> {
                    try {
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        started.set(false);
                        try {
                            webcamPanel.removeAll();
                            webcamPanel.revalidate();
                            webcamPanel.repaint();
                        } catch (Throwable ignored) {
                        }
                    } catch (Throwable ignored) {
                    }
                });
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                started.set(false);
                try {
                    webcamPanel.removeAll();
                    webcamPanel.revalidate();
                    webcamPanel.repaint();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private void updateMenusForRole() {
        boolean isAdmin = accountRole == Role.ADMIN;
        boolean isCaretaker = accountRole == Role.CARETAKER;
        boolean isGuest = accountRole == Role.GUEST;

        adoptBtn.setVisible(isGuest);
        edcatinfoBtn.setVisible(isAdmin || isCaretaker);
        ADpanel.setVisible(isAdmin);
    }

    public static void setGlobalFont(Font font) {
        FontUIResource fontRes = new FontUIResource(font);
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }
        UIManager.put("defaultFont", fontRes);
        UIManager.put("Component.font", fontRes);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        webcamPanel = new javax.swing.JPanel();
        ProfBtn = new javax.swing.JButton();
        MapBtn = new javax.swing.JButton();
        edcatinfoBtn = new javax.swing.JButton();
        DarkToggBtn = new javax.swing.JToggleButton();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        ADpanel = new javax.swing.JPanel();
        ADeditCtakersBtn = new javax.swing.JButton();
        testcatprofilemenu = new javax.swing.JButton();
        adoptBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("UNP Cat-alog");
        setMaximumSize(new java.awt.Dimension(1280, 720));
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setPreferredSize(new java.awt.Dimension(1280, 740));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        webcamPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout webcamPanelLayout = new javax.swing.GroupLayout(webcamPanel);
        webcamPanel.setLayout(webcamPanelLayout);
        webcamPanelLayout.setHorizontalGroup(
            webcamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 858, Short.MAX_VALUE)
        );
        webcamPanelLayout.setVerticalGroup(
            webcamPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 618, Short.MAX_VALUE)
        );

        getContentPane().add(webcamPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 20, 860, 620));

        ProfBtn.setText("Profile");
        ProfBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ProfBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ProfBtnActionPerformed(evt);
            }
        });
        getContentPane().add(ProfBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 40, 80, 30));

        MapBtn.setText("Map");
        MapBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        MapBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapBtnActionPerformed(evt);
            }
        });
        getContentPane().add(MapBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 270, 270, 40));

        edcatinfoBtn.setText("Edit Cat Profile");
        edcatinfoBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        edcatinfoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edcatinfoBtnActionPerformed(evt);
            }
        });
        getContentPane().add(edcatinfoBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 210, 270, 40));

        DarkToggBtn.setText("Dark Mode");
        DarkToggBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DarkToggBtnActionPerformed(evt);
            }
        });
        getContentPane().add(DarkToggBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 40, -1, -1));

        startButton.setText("Start");
        startButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });
        getContentPane().add(startButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 650, 290, -1));

        stopButton.setText("Stop");
        stopButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        getContentPane().add(stopButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 650, 290, -1));
        stopButton.setEnabled(false);

        ADpanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Admin Panel", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial Black", 0, 12))); // NOI18N

        ADeditCtakersBtn.setText("Edit Caretakers");
        ADeditCtakersBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ADeditCtakersBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ADeditCtakersBtnActionPerformed(evt);
            }
        });

        testcatprofilemenu.setText("Test Cat Profile");
        testcatprofilemenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testcatprofilemenuActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ADpanelLayout = new javax.swing.GroupLayout(ADpanel);
        ADpanel.setLayout(ADpanelLayout);
        ADpanelLayout.setHorizontalGroup(
            ADpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ADpanelLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(ADpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ADeditCtakersBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)
                    .addComponent(testcatprofilemenu, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(35, 35, 35))
        );
        ADpanelLayout.setVerticalGroup(
            ADpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ADpanelLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(ADeditCtakersBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(testcatprofilemenu, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        getContentPane().add(ADpanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 420, 350, 180));

        adoptBtn.setText("Want to adopt a cat?");
        adoptBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        adoptBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adoptBtnActionPerformed(evt);
            }
        });
        getContentPane().add(adoptBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 330, 270, 40));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void MapBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MapBtnActionPerformed
        mapMenu map = new mapMenu();
        map.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        map.setVisible(true);
        MapBtn.setEnabled(false);

        map.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                javax.swing.SwingUtilities.invokeLater(() -> MapBtn.setEnabled(true));
            }
        });
    }//GEN-LAST:event_MapBtnActionPerformed

    private void ProfBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ProfBtnActionPerformed
        if (accountId != null) {
            ProfileMenu profMenu = new ProfileMenu(accountId);
            profMenu.setVisible(true);
        } else {
            loginMenu logMenu = new loginMenu();
            logMenu.setVisible(true);
        }
    }//GEN-LAST:event_ProfBtnActionPerformed

    private void DarkToggBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DarkToggBtnActionPerformed
        try {
            if (!darkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                darkMode = true;
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                darkMode = false;
            }

            // Update all open windows so the new L&F takes effect immediately
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
                w.invalidate();
                w.validate();
                w.repaint();
                if (w instanceof JFrame) {
                    ((JFrame) w).pack();
                } else if (w instanceof JDialog) {
                    ((JDialog) w).pack();
                }
            }
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to change theme:\n" + ex.getMessage(),
                    "Theme error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_DarkToggBtnActionPerformed

    private void edcatinfoBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edcatinfoBtnActionPerformed
        catProfileMenu catProf = new catProfileMenu(0);
        catProf.setVisible(true);

        int aid = 0;
        if (accountId != null) {
            try (Connection conn = dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT caretaker_id FROM accounts WHERE account_id = ?")) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int caretakerId = rs.getInt("caretaker_id");
                        if (!rs.wasNull()) {
                            aid = caretakerId;
                        } else {
                            aid = 0;
                        }
                    } else {
                        aid = 0;
                    }
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to lookup caretaker_id for account {0}", new Object[]{accountId});
                logger.log(java.util.logging.Level.FINE, "SQLException while retrieving caretaker_id", ex);
                JOptionPane.showMessageDialog(this,
                        "Failed to determine caretaker mapping:\n" + ex.getMessage(),
                        "DB error",
                        JOptionPane.ERROR_MESSAGE);
                aid = 0;
            }
        }

        editCatMenu edMenu = new editCatMenu(aid, catProf);
        edMenu.setVisible(true);
    }//GEN-LAST:event_edcatinfoBtnActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (started.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
            });
            startQRPrev();
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        if (started.compareAndSet(true, false)) {
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                try {
                    webcamPanel.removeAll();
                    webcamPanel.revalidate();
                    webcamPanel.repaint();
                } catch (Throwable ignored) {
                }
            });
            submitCleanup();
        }
    }//GEN-LAST:event_stopButtonActionPerformed

    private void testcatprofilemenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testcatprofilemenuActionPerformed
        catProfileMenu.testMenu();
    }//GEN-LAST:event_testcatprofilemenuActionPerformed

    private void ADeditCtakersBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ADeditCtakersBtnActionPerformed
        editCaretakersMenu edCaretakersMenu = new editCaretakersMenu();
        edCaretakersMenu.setVisible(true);
    }//GEN-LAST:event_ADeditCtakersBtnActionPerformed

    private void adoptBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adoptBtnActionPerformed
        adoptMenu adopt = new adoptMenu();
        adopt.setVisible(true);
    }//GEN-LAST:event_adoptBtnActionPerformed

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());

            setGlobalFont(new Font("Arial", Font.PLAIN, 12));
            java.awt.EventQueue.invokeLater(() -> {
                try {
                    new qrMenu("", "").setVisible(true);
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE, "Failed to launch qrMenu", t);
                }
            });

        } catch (UnsupportedLookAndFeelException ex) {
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ADeditCtakersBtn;
    private javax.swing.JPanel ADpanel;
    private javax.swing.JToggleButton DarkToggBtn;
    private javax.swing.JButton MapBtn;
    private javax.swing.JButton ProfBtn;
    private javax.swing.JButton adoptBtn;
    private javax.swing.JButton edcatinfoBtn;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JButton testcatprofilemenu;
    private javax.swing.JPanel webcamPanel;
    // End of variables declaration//GEN-END:variables
}

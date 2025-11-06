package main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import java.awt.Font;
import java.awt.Window;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import main.stuff.QRstuff;

public class qrMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(qrMenu.class.getName());

    private QRstuff qrStuff;
    private WebcamPanel previewPanel;
    private boolean darkMode = false;
    private String accountType = "";

    // Single-thread cleanup executor (daemon) to run potentially blocking resource cleanup off the EDT.
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-preview-cleanup");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean windowListenerAdded = new AtomicBoolean(false);

    private final AtomicBoolean started = new AtomicBoolean(false);

    public qrMenu() {
        initComponents();
        checkAccountType();
    }

    private void startQRPrev() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Create QRStuff (uses default webcam)
                qrStuff = new QRstuff();

                // Obtain the WebcamPanel provided by QRStuff
                previewPanel = qrStuff.getWebcamPanel();

                // Configure preview to scale to fill the component
                previewPanel.setFillArea(true);

                // Put the preview into the existing jPanel1
                jPanel1.removeAll();
                jPanel1.setLayout(new BorderLayout());
                jPanel1.add(previewPanel, BorderLayout.CENTER);

                // Ensure preview initially matches panel size (if already laid out)
                Dimension initial = jPanel1.getSize();
                if (initial == null || initial.width == 0 || initial.height == 0) {
                    initial = jPanel1.getPreferredSize();
                    if (initial == null || initial.width == 0 || initial.height == 0) {
                        initial = new Dimension(640, 480);
                    }
                }
                previewPanel.setPreferredSize(initial);
                previewPanel.setSize(initial);
                previewPanel.revalidate();

                // Listen for jPanel1 resize events and adapt preview + webcam
                jPanel1.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        Dimension newSize = jPanel1.getSize();
                        if (newSize == null) {
                            return;
                        }

                        // Update preview panel sizing so the displayed image scales
                        try {
                            previewPanel.setPreferredSize(newSize);
                            previewPanel.setSize(newSize);
                            previewPanel.revalidate();
                        } catch (Throwable ignored) {
                        }

                        // Attempt to set webcam view size to closest supported resolution for better quality.
                        try {
                            Webcam camera = qrStuff.getWebcam();
                            if (camera != null) {
                                Dimension best = findClosestSize(camera, newSize);
                                if (best != null) {
                                    Dimension current = camera.getViewSize();
                                    if (current == null || !current.equals(best)) {
                                        camera.setViewSize(best);
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                            // don't break UI on failure; just log at FINE
                            logger.log(Level.FINE, "Failed to adjust webcam view size", ex);
                        }
                    }
                });

                // Refresh layout
                pack();
                revalidate();
                repaint();

                // Start scanning (caller may choose to start later instead)
                try {
                    qrStuff.startScanning();
                } catch (Throwable t) {
                    logger.log(Level.FINE, "qrStuff.startScanning threw", t);
                }

                // Optional: register callbacks (console logging)
                try {
                    qrStuff.setDecodedCallback(text -> System.out.println("Decoded: " + text));
                    qrStuff.setCatIdCallback(id -> System.out.println("Cat ID: " + id));
                    qrStuff.setStatusCallback(status -> System.out.println("Status: " + status));
                } catch (Throwable t) {
                    // ignore if callbacks not present
                }

            } catch (NoClassDefFoundError ncd) {
                logger.severe("Missing native/library dependency: " + ncd.getMessage());
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Failed to initialize QR preview", t);
            }
        });

        // Ensure QRStuff is disposed when the window closes
        if (windowListenerAdded.compareAndSet(false, true)) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Quick UI work on EDT to detach heavy components so the window can close fast.
                    SwingUtilities.invokeLater(() -> {
                        try {
                            jPanel1.removeAll();
                            jPanel1.revalidate();
                            jPanel1.repaint();
                        } catch (Throwable ignored) {
                        }
                    });

                    // Perform heavy cleanup asynchronously so the EDT isn't blocked.
                    submitCleanup();
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    submitCleanup();
                }
            });
        }
    }

    private void submitCleanup() {
        cleanupExecutor.submit(() -> {
            try {
                if (qrStuff != null) {
                    try {
                        qrStuff.stopScanning();
                    } catch (Throwable t) {
                        logger.log(Level.FINE, "stopScanning threw", t);
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "Failed while attempting to stop scanning", t);
            }

            try {
                if (previewPanel != null) {
                    try {
                        previewPanel.stop();
                    } catch (Throwable t) {
                        logger.log(Level.FINE, "previewPanel.stop threw", t);
                    } finally {
                        previewPanel = null;
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "Failed while stopping previewPanel", t);
            }

            try {
                if (qrStuff != null) {
                    try {
                        qrStuff.dispose();
                    } catch (Throwable t) {
                        logger.log(Level.FINE, "qrStuff.dispose threw", t);
                    } finally {
                        qrStuff = null;
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "Failed while disposing qrStuff", t);
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    started.set(false);
                } catch (Throwable ignored) {
                }
            });

            // Optionally shutdown the executor if you never plan to reopen the preview during the JVM lifetime.
            // cleanupExecutor.shutdown(); // uncomment if appropriate
        });
    }

    private Dimension findClosestSize(Webcam camera, Dimension target) {
        if (camera == null || target == null) {
            return null;
        }
        Dimension[] supported = camera.getViewSizes();
        if (supported == null || supported.length == 0) {
            return null;
        }
        Dimension best = supported[0];
        long bestDiff = Math.abs(best.width - target.width) + Math.abs(best.height - target.height);
        for (int i = 1; i < supported.length; i++) {
            Dimension d = supported[i];
            long diff = Math.abs(d.width - target.width) + Math.abs(d.height - target.height);
            if (diff < bestDiff) {
                best = d;
                bestDiff = diff;
            }
        }
        return best;
    }

    private void checkAccountType() {
        if (accountType.equals("admin")) {
            edcatinfoBtn.setVisible(true);
            ADeditCtakersBtn.setVisible(true);
        } else if (accountType.equals("caretaker")) {
            edcatinfoBtn.setVisible(true);
            ADeditCtakersBtn.setVisible(false);
        } else {
            edcatinfoBtn.setVisible(false);
            ADeditCtakersBtn.setVisible(false);
        }
    }

    public static void setGlobalFont(Font font) {
        FontUIResource fontRes = new FontUIResource(font);

        // Preferred: replace only FontUIResource entries in UIDefaults
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }

        // Some LaFs use "defaultFont" or "Component.font" keys; set them too
        UIManager.put("defaultFont", fontRes);
        UIManager.put("Component.font", fontRes);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        ProfBtn = new javax.swing.JButton();
        ADeditCtakersBtn = new javax.swing.JButton();
        MapBtn = new javax.swing.JButton();
        edcatinfoBtn = new javax.swing.JButton();
        DarkToggBtn = new javax.swing.JToggleButton();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("UNP Cat-alog");
        setMaximumSize(new java.awt.Dimension(1280, 720));
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setPreferredSize(new java.awt.Dimension(1280, 740));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 858, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 618, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 20, 860, 620));

        ProfBtn.setText("Profile");
        ProfBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ProfBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ProfBtnActionPerformed(evt);
            }
        });
        getContentPane().add(ProfBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 40, 80, 30));

        ADeditCtakersBtn.setText("Edit Caretakers");
        ADeditCtakersBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        getContentPane().add(ADeditCtakersBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 260, 270, 40));

        MapBtn.setText("Map");
        MapBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        MapBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MapBtnActionPerformed(evt);
            }
        });
        getContentPane().add(MapBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 330, 270, 40));

        edcatinfoBtn.setText("Edit Cat Profile");
        edcatinfoBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        edcatinfoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edcatinfoBtnActionPerformed(evt);
            }
        });
        getContentPane().add(edcatinfoBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 400, 270, 40));

        DarkToggBtn.setText("Dark Mode");
        DarkToggBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DarkToggBtnActionPerformed(evt);
            }
        });
        getContentPane().add(DarkToggBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 640, -1, -1));

        startButton.setText("Start");
        startButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });
        getContentPane().add(startButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 660, -1, -1));

        stopButton.setText("Stop");
        stopButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        getContentPane().add(stopButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 660, -1, -1));
        stopButton.setEnabled(false);

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 560, -1, -1));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void MapBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MapBtnActionPerformed
        mapMenu map = new mapMenu();
        map.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        map.setVisible(true);
        MapBtn.setEnabled(true);
    }//GEN-LAST:event_MapBtnActionPerformed

    private void ProfBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ProfBtnActionPerformed
//        if (account_id )
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
                // If window sizes depend on L&F, re-pack frames to adjust sizes
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
        // TODO add your handling code here:
    }//GEN-LAST:event_edcatinfoBtnActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        // Only allow start action if not already started
        if (started.compareAndSet(false, true)) {
            // Update button states immediately so user sees feedback.
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            });
            // Start the preview/scanner
            startQRPrev();
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        if (started.compareAndSet(true, false)) {
            // Quick UI detach so window can close fast/stop rendering preview
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                try {
                    jPanel1.removeAll();
                    jPanel1.revalidate();
                    jPanel1.repaint();
                } catch (Throwable ignored) {
                }
            });

            // Perform heavy cleanup asynchronously
            submitCleanup();
        }
    }//GEN-LAST:event_stopButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        CatProfileMenu catProf = new CatProfileMenu(2);
        catProf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        catProf.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    public static void main(String[] args) {
        try {
            // Set the desired FlatLaf look and feel
            UIManager.setLookAndFeel(new FlatLightLaf());

            setGlobalFont(new Font("Arial", Font.PLAIN, 13));
            java.awt.EventQueue.invokeLater(() -> {
                try {
                    new qrMenu().setVisible(true);
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE, "Failed to launch qrMenu", t);
                }
            });

        } catch (UnsupportedLookAndFeelException ex) {
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ADeditCtakersBtn;
    private javax.swing.JToggleButton DarkToggBtn;
    private javax.swing.JButton MapBtn;
    private javax.swing.JButton ProfBtn;
    private javax.swing.JButton edcatinfoBtn;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    // End of variables declaration//GEN-END:variables
}

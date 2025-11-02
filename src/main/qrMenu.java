package main;

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
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * qrMenu frame with embedded QR preview that follows the jPanel size.
 *
 * This file keeps generated GUI code inside initComponents() untouched (a
 * minimal initComponents is present so the file is self-contained).
 *
 * The important behaviour: - startQRPrev() embeds QRStuff's WebcamPanel into
 * jPanel1 - the preview image is set to fill the jPanel (setFillArea(true)) - a
 * ComponentListener watches jPanel1 resizes and: * updates the preview panel
 * preferred size so Swing scales the image * attempts to set the webcam view
 * size to the closest supported resolution - disposeQRStuff() stops and
 * disposes the scanner/panel when window closes
 */
public class qrMenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(qrMenu.class.getName());

    // non-generated fields
    private QRstuff qrStuff;
    private WebcamPanel previewPanel;

    /**
     * Creates new form qrMenu
     */
    public qrMenu() {
        initComponents();
        startQRPrev();
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
                qrStuff.startScanning();

                // Optional: register callbacks (console logging)
                qrStuff.setDecodedCallback(text -> System.out.println("Decoded: " + text));
                qrStuff.setCatIdCallback(id -> System.out.println("Cat ID: " + id));
                qrStuff.setStatusCallback(status -> System.out.println("Status: " + status));

            } catch (NoClassDefFoundError ncd) {
                logger.severe("Missing native/library dependency: " + ncd.getMessage());
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Failed to initialize QR preview", t);
            }
        });

        // Ensure QRStuff is disposed when the window closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disposeQRStuff();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                disposeQRStuff();
            }
        });
    }

    private void disposeQRStuff() {
        if (qrStuff != null) {
            try {
                qrStuff.dispose();
            } catch (Throwable ignored) {
            } finally {
                qrStuff = null;
            }
        }
        if (previewPanel != null) {
            try {
                previewPanel.stop();
            } catch (Throwable ignored) {
            } finally {
                previewPanel = null;
            }
        }
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1280, 720));
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setPreferredSize(new java.awt.Dimension(1280, 720));
        setResizable(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 690, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 620, Short.MAX_VALUE)
        );

        jButton1.setText("jButton1");

        jButton2.setText("jButton2");

        jButton3.setText("jButton3");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(120, 120, 120)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton2)
                            .addComponent(jButton3))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 348, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(38, 38, 38))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(119, 119, 119)
                        .addComponent(jButton1)
                        .addGap(62, 62, 62)
                        .addComponent(jButton2)
                        .addGap(66, 66, 66)
                        .addComponent(jButton3))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(59, 59, 59))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String[] args) {
        try {
            // Set the desired FlatLaf look and feel
            UIManager.setLookAndFeel(new FlatLightLaf());

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
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}

package main;

/*
 * QRWebcamScannerUI.java
 *
 * Single-class webcam QR scanner + preview that:
 *  - shows a live webcam preview (using sarxos webcam-capture WebcamPanel)
 *  - continuously attempts ZXing decoding on frames
 *  - when a QR is decoded and a numeric cat_id is parsed, opens CatProfileMenu(cat_id)
 *
 * Requirements (put jars in lib/ and on classpath):
 *  - com.google.zxing: core + javase (e.g. core-3.5.1.jar, javase-3.5.1.jar)
 *  - com.github.sarxos:webcam-capture (e.g. webcam-capture-0.3.12.jar) and its dependencies (jna etc.)
 *  - MySQL/MariaDB JDBC (mysql-connector-java)
 *  - Your project must compile main.stuff.dbconn and CatProfileMenu must be on the classpath
 *
 * Usage:
 *  - Run from IDE or command line. The UI will open and start scanning automatically.
 *  - When a valid cat_id is decoded the CatProfileMenu will open on the Event Dispatch Thread.
 *
 * Notes:
 *  - This class uses the project's db helper indirectly by opening CatProfileMenu, which
 *    uses main.stuff.dbconn.getConnection() per your project.
 *  - If you want the scanner integrated inside an existing menu, add the returned
 *    WebcamPanel (scanner.getWebcamPanel()) into your menu's layout instead of creating
 *    a top-level window.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import javax.swing.JOptionPane;

/* No package declaration so it can instantiate CatProfileMenu in default package.
   If your CatProfileMenu is in a package, update the new CatProfileMenu(catId)
   call to use the fully-qualified name or move this class to the same package. */

public class QRWebcamScannerUI extends JFrame {

    // regex to support plain "123" or URL ending with /cat/123 or /cats/123
    private static final Pattern ID_EXTRACT = Pattern.compile(".*/(cat|cats)/?(\\d+)$|^(\\d+)$");

    // debounce interval in ms to avoid repeated opens
    private static final long DEBOUNCE_MS = 3000L;

    private final Webcam webcam;
    private final WebcamPanel webcamPanel;
    private final JLabel lblStatus = new JLabel("Initializing...");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnClose = new JButton("Close");

    private final ExecutorService decoderExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private String lastDecoded = null;
    private long lastTimeMillis = 0;

    public QRWebcamScannerUI() throws Exception {
        super("QR Webcam Scanner");
        // open default webcam
        webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("No webcam detected on this machine.");
        }
        // pick a reasonable size (you can change)
        Dimension size = Webcam.getDefault().getViewSizes().length > 0 ? Webcam.getDefault().getViewSizes()[0] : new Dimension(640, 480);
        try {
            webcam.setViewSize(size);
        } catch (Throwable ignored) {}

        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setMirrored(false);
        webcamPanel.setPreferredSize(new Dimension(640, 480));

        initGui();
        startScanning();
    }

    private void initGui() {
        setLayout(new BorderLayout(6, 6));

        JPanel previewWrap = new JPanel(new BorderLayout());
        previewWrap.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        previewWrap.add(webcamPanel, BorderLayout.CENTER);

        // status panel
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 14f));
        lblStatus.setForeground(Color.BLUE);
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.add(Box.createHorizontalStrut(8));
        statusPanel.add(lblStatus);
        statusPanel.add(Box.createHorizontalGlue());
        statusPanel.add(btnStop);
        statusPanel.add(Box.createHorizontalStrut(6));
        statusPanel.add(btnClose);
        statusPanel.add(Box.createHorizontalStrut(8));

        add(previewWrap, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        btnStop.addActionListener(e -> {
            if (running.get()) {
                stopScanning();
                btnStop.setText("Start");
                lblStatus.setText("Stopped");
            } else {
                startScanning();
                btnStop.setText("Stop");
                lblStatus.setText("Scanning...");
            }
        });

        btnClose.addActionListener(e -> {
            stopScanning();
            dispose();
        });

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void startScanning() {
        if (running.compareAndSet(false, true)) {
            lblStatus.setText("Scanning...");
            decoderExecutor.submit(this::decodeLoop);
        }
    }

    private void stopScanning() {
        running.set(false);
        // do not shutdown executor here — keep it for possible restart; could shutdown on dispose
    }

    private void decodeLoop() {
        MultiFormatReader reader = new MultiFormatReader();
        while (running.get()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image == null) {
                    Thread.sleep(100);
                    continue;
                }
                try {
                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result result = reader.decode(bitmap);
                    if (result != null) {
                        final String text = result.getText();
                        handleDecodedText(text);
                    }
                } catch (NotFoundException nf) {
                    // no QR found in this frame - ignore
                } catch (Throwable decodeErr) {
                    // unexpected decode error; show status but continue
                    updateStatus("Decode error: " + decodeErr.getMessage());
                }
                Thread.sleep(150); // tune for CPU / responsiveness
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                updateStatus("Camera error: " + t.getMessage());
                // if camera fails, stop scanning
                running.set(false);
                break;
            }
        }
        updateStatus("Scanner stopped.");
    }

    private void handleDecodedText(String decoded) {
        if (decoded == null) return;
        long now = System.currentTimeMillis();
        boolean isNew = !decoded.equals(lastDecoded) || (now - lastTimeMillis) > DEBOUNCE_MS;
        if (!isNew) return;
        lastDecoded = decoded;
        lastTimeMillis = now;

        updateStatus("Decoded: " + decoded);

        Integer catId = extractCatId(decoded);
        if (catId != null) {
            updateStatus("Cat ID: " + catId + " — opening profile...");
            // open CatProfileMenu on EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    // instantiate your CatProfileMenu (must be on classpath)
                    CatProfileMenu profile = new CatProfileMenu(catId);
                    profile.setVisible(true);
                } catch (Throwable t) {
                    // if CatProfileMenu constructor throws, show message
                    updateStatus("Failed to open profile: " + t.getMessage());
                    t.printStackTrace();
                }
            });
        } else {
            updateStatus("QR decoded but did not contain a numeric cat_id.");
        }
    }

    private Integer extractCatId(String text) {
        if (text == null) return null;
        text = text.trim();
        Matcher m = ID_EXTRACT.matcher(text);
        if (m.find()) {
            String g1 = m.group(2);
            String g2 = m.group(3);
            String idStr = g1 != null ? g1 : g2;
            if (idStr != null) {
                try {
                    return Integer.parseInt(idStr);
                } catch (NumberFormatException ignored) {}
            }
        }
        // fallback: entire text is integer
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    /**
     * If you want to embed the preview into an existing menu, call getWebcamPanel()
     * and add it into your layout instead of creating a top-level window.
     */
    public WebcamPanel getWebcamPanel() {
        return webcamPanel;
    }

    /**
     * Call when shutting down the app to release resources.
     */
    public void disposeScanner() {
        stopScanning();
        try {
            decoderExecutor.shutdownNow();
        } catch (Throwable ignored) {}
        try {
            if (webcam != null && webcam.isOpen()) webcam.close();
        } catch (Throwable ignored) {}
    }

    // quick launcher
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                QRWebcamScannerUI scanner = new QRWebcamScannerUI();
                scanner.setVisible(true);
            } catch (NoClassDefFoundError ncd) {
                String missing = ncd.getMessage();
                JOptionPane.showMessageDialog(null,
                        "Missing library: " + missing + "\n\nMake sure ZXing and webcam-capture jars are on the classpath (lib/).",
                        "Dependency missing", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start scanner: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import javax.swing.JOptionPane;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

public class eyqr extends JFrame {

    // only accepts "cat_id = 123" pattern
    private static final Pattern CAT_ID_EXACT = Pattern.compile("(?i)^\\s*cat_id\\s*=\\s*(\\d+)\\s*$");

    private static final long DEBOUNCE_MS = 3000L;

    private final Webcam webcam;
    private final WebcamPanel webcamPanel;
    private final JLabel lblStatus = new JLabel("Initializing...");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnClose = new JButton("Close");

    private final ExecutorService decoderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-decoder");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);

    private String lastDecoded = null;
    private long lastTimeMillis = 0;

    public eyqr() throws Exception {
        super("QRtest");
        webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("No webcam detected on this machine.");
        }
        //
        Dimension size = Webcam.getDefault().getViewSizes().length > 0 ? Webcam.getDefault().getViewSizes()[0] : new Dimension(640, 480);
        try {
            webcam.setViewSize(size);
        } catch (Throwable ignored) {
        }

        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(false);
        webcamPanel.setMirrored(false);
        webcamPanel.setPreferredSize(new Dimension(1280, 720));

        initGui();

        attachShutdownListener();

        startScanning();
    }

    private void initGui() {
        setLayout(new BorderLayout(6, 6));

        JPanel previewWrap = new JPanel(new BorderLayout());
        previewWrap.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        previewWrap.add(webcamPanel, BorderLayout.CENTER);

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
            disposeScanner();
            dispose();
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
                    // no QR in this frame - ignore
                } catch (Throwable decodeErr) {
                    updateStatus("Decode error: " + decodeErr.getMessage());
                }
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                updateStatus("Camera error: " + t.getMessage());
                running.set(false);
                break;
            }
        }
        updateStatus("Scanner stopped.");
    }

    private void handleDecodedText(String decoded) {
        if (decoded == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean isNew = !decoded.equals(lastDecoded) || (now - lastTimeMillis) > DEBOUNCE_MS;
        if (!isNew) {
            return;
        }
        lastDecoded = decoded;
        lastTimeMillis = now;

        updateStatus("Decoded: " + decoded);

        Integer catId = extractCatId(decoded);
        if (catId != null) {
            updateStatus("Cat ID: " + catId + " — opening profile...");
            System.out.println(decoded);
            SwingUtilities.invokeLater(() -> {
                try {
                    CatProfileMenu profile = new CatProfileMenu(catId);
                    profile.setVisible(true);
                } catch (Throwable t) {
                    updateStatus("Failed to open profile: " + t.getMessage());
                    t.printStackTrace();
                }
            });
        } else {
            updateStatus("QR decoded but did not contain a numeric cat_id.");
            System.out.println(decoded);
        }
    }

    private static Integer extractCatId(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = CAT_ID_EXACT.matcher(text);
        if (m.matches()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    public WebcamPanel getWebcamPanel() {
        return webcamPanel;
    }

    public void disposeScanner() {
        stopScanning();
        try {
            if (webcamPanel != null) {
                webcamPanel.stop();
            }
        } catch (Throwable ignored) {
        }

        try {
            decoderExecutor.shutdownNow();
            if (!decoderExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                decoderExecutor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Throwable ignored) {
        }

        try {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        } catch (Throwable ignored) {
        }
    }

    private void attachShutdownListener() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                disposeScanner();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                disposeScanner();
            }
        });
    }

    // test
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                eyqr scanner = new eyqr();
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

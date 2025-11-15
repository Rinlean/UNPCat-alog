package main.stuff;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import main.catProfileMenu;

public class QRstuff {

    // Accepts strict "cat_id = 123"
    private static final Pattern CAT_ID_EXACT = Pattern.compile("(?i)^\\s*cat_id\\s*=\\s*(\\d+)\\s*$");
    // Accept things like "...cat_id:123..." or "...cat-id=123..." (looser)
    private static final Pattern CAT_ID_LOOSE = Pattern.compile("(?i).*\\bcat(?:_|-)?id\\b\\s*[:=]?\\s*(\\d+).*");
    // Accept "/cat/123" style urls or ".../cat/123/..."
    private static final Pattern CAT_URL = Pattern.compile("(?i).*/cat/(\\d+).*");
    // Simple fallback: matches "id=123", "cat: 123", etc.
    private static final Pattern GENERIC_ID = Pattern.compile("(?i).*\\b(?:id|cat)\\b\\s*[:=]?\\s*(\\d+).*");

    private static final long DEBOUNCE_MS = 3000L;

    private final Webcam webcam;
    private WebcamPanel webcamPanel;

    private final ExecutorService decoderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-decoder");
        t.setDaemon(true);
        return t;
    });

    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-cleanup");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);

    private String lastDecoded;
    private long lastTimeMillis;

    private Consumer<String> decodedCallback;
    private Consumer<Integer> catIdCallback;
    private Consumer<String> statusCallback;

    private volatile boolean autoOpenProfile = true;

    private final Map<DecodeHintType, Object> decodeHints;

    public QRstuff() {
        this(Webcam.getDefault());
    }

    public QRstuff(Webcam webcam) {
        if (webcam == null) {
            throw new IllegalStateException("No webcam available.");
        }
        this.webcam = webcam;

        decodeHints = new EnumMap<>(DecodeHintType.class);
        decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        decodeHints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

        try {
            if (webcam.getViewSizes().length > 0) {
                webcam.setViewSize(webcam.getViewSizes()[0]);
            } else {
                webcam.setViewSize(new Dimension(640, 480));
            }
        } catch (Throwable ignored) {
        }
        notifyStatus("QRStuff initialized");
    }

    public synchronized WebcamPanel getWebcamPanel() {
        if (webcamPanel != null) {
            return webcamPanel;
        }
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(false);
        webcamPanel.setMirrored(false);

        try {
            webcamPanel.start();
        } catch (Throwable t) {
            notifyStatus("Failed to start webcam preview: " + t.getMessage());
        }
        return webcamPanel;
    }

    public synchronized void attachToPanel(JPanel parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent panel must not be null");
        }

        WebcamPanel panel = getWebcamPanel(); // creates and starts panel if needed
        panel.setFillArea(true);

        parent.removeAll();
        parent.setLayout(new BorderLayout());
        parent.add(panel, BorderLayout.CENTER);

        Dimension initial = parent.getSize();
        if (initial == null || initial.width == 0 || initial.height == 0) {
            initial = parent.getPreferredSize();
            if (initial == null || initial.width == 0 || initial.height == 0) {
                initial = new Dimension(640, 480);
            }
        }
        panel.setPreferredSize(initial);
        panel.setSize(initial);
        panel.revalidate();

        // Listen for parent resizes and adapt preview + webcam view size
        parent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension newSize = parent.getSize();
                if (newSize == null) {
                    return;
                }

                try {
                    panel.setPreferredSize(newSize);
                    panel.setSize(newSize);
                    panel.revalidate();
                } catch (Throwable ignored) {
                }

                try {
                    Webcam camera = getWebcam();
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
                    notifyStatus("Failed to adjust webcam view size: " + ex.getMessage());
                }
            }
        });

        // ensure layout refresh
        parent.revalidate();
        parent.repaint();
    }

    public void startScanning() {
        if (running.compareAndSet(false, true)) {
            notifyStatus("Scanning...");
            decoderExecutor.submit(this::decodeLoop);
        }
    }

    public void stopScanning() {
        running.set(false);
    }

    private void decodeLoop() {
        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(decodeHints);

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
                    Result result = reader.decodeWithState(bitmap);
                    if (result != null) {
                        handleDecodedText(result.getText());
                    }
                } catch (NotFoundException nf) {
                    // no QR in this frame - ignore
                } catch (Throwable decodeErr) {
                    notifyStatus("Decode error: " + decodeErr.getMessage());
                }
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                notifyStatus("Camera error: " + t.getMessage());
                running.set(false);
                break;
            }
        }
        notifyStatus("Scanner stopped.");
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

        notifyStatus("Decoded: " + decoded);

        if (decodedCallback != null) {
            try {
                decodedCallback.accept(decoded);
            } catch (Throwable ignored) {
            }
        }

        Integer catId = extractCatId(decoded);

        if (catId != null) {
            if (catIdCallback != null) {
                try {
                    catIdCallback.accept(catId);
                } catch (Throwable ignored) {
                }
            }

            if (autoOpenProfile) {
                final Integer idToOpen = catId;
                SwingUtilities.invokeLater(() -> {
                    try {
                        catProfileMenu profile = new catProfileMenu(idToOpen);
                        profile.setVisible(true);
                    } catch (Throwable t) {
                        notifyStatus("Failed to open profile: " + t.getMessage());
                    }
                });
            }
        }
    }

    public static Integer extractCatId(String text) {
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
        m = CAT_ID_LOOSE.matcher(text);
        if (m.matches()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        m = CAT_URL.matcher(text);
        if (m.matches()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        m = GENERIC_ID.matcher(text);
        if (m.matches()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // Callbacks registration
    public void setDecodedCallback(Consumer<String> cb) {
        this.decodedCallback = cb;
    }

    public void setCatIdCallback(Consumer<Integer> cb) {
        this.catIdCallback = cb;
    }

    public void setStatusCallback(Consumer<String> cb) {
        this.statusCallback = cb;
    }

    public void setAutoOpenProfile(boolean autoOpen) {
        this.autoOpenProfile = autoOpen;
    }

    private void notifyStatus(String s) {
        if (statusCallback != null) {
            try {
                statusCallback.accept(s);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispose() {
        stopScanning();

        try {
            decoderExecutor.shutdownNow();
            if (!decoderExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                decoderExecutor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            try {
                decoderExecutor.shutdownNow();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        if (webcamPanel != null) {
            try {
                webcamPanel.stop();
            } catch (Throwable ignored) {
            }
            webcamPanel = null;
        }

        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            } catch (Throwable ignored) {
            }
        }
        notifyStatus("Disposed");
    }

    public void submitCleanup(Runnable onFinished) {
        cleanupExecutor.submit(() -> {
            try {
                dispose();
            } catch (Throwable t) {
                notifyStatus("Error during cleanup: " + t.getMessage());
            } finally {
                try {
                    cleanupExecutor.shutdown();
                    if (!cleanupExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                        cleanupExecutor.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    cleanupExecutor.shutdownNow();
                } catch (Throwable ignored) {
                }
            }

            if (onFinished != null) {
                SwingUtilities.invokeLater(onFinished);
            }
        });
    }

    public void submitCleanup() {
        submitCleanup(null);
    }

    public Webcam getWebcam() {
        return webcam;
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
}
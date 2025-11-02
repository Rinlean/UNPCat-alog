package main;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

/**
 * QRStuff: Non-UI QR scanner that provides a WebcamPanel for embedding into a Swing container.
 * - Keeps decoding logic separate from UI.
 * - Caller (qrMenu) is responsible for adding the returned WebcamPanel into its jPanel and
 *   for calling dispose() when the window closes.
 */
public class QRstuff {

    // only accepts "cat_id = 123" pattern
    private static final Pattern CAT_ID_EXACT = Pattern.compile("(?i)^\\s*cat_id\\s*=\\s*(\\d+)\\s*$");
    private static final long DEBOUNCE_MS = 3000L;

    private final Webcam webcam;
    private WebcamPanel webcamPanel;

    private final ExecutorService decoderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-decoder");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);

    private String lastDecoded;
    private long lastTimeMillis;

    // callbacks
    private Consumer<String> decodedCallback;
    private Consumer<Integer> catIdCallback;
    private Consumer<String> statusCallback;

    /**
     * Create QRStuff and use the default webcam.
     * Does not start scanning automatically.
     * @throws IllegalStateException when no webcam is available
     */
    public QRstuff() {
        this(Webcam.getDefault());
    }

    /**
     * Create QRStuff using an existing Webcam instance (useful when caller wants to share camera).
     * Does not start scanning automatically.
     * @param webcam non-null Webcam instance
     */
    public QRstuff(Webcam webcam) {
        if (webcam == null) {
            throw new IllegalStateException("No webcam available.");
        }
        this.webcam = webcam;
        // prefer a reasonable default view size if available
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

    /**
     * Returns a configured WebcamPanel that you can add to your Swing layout (for example jPanel1).
     * The panel will be created lazily and returned. The panel is also automatically started here
     * so it begins previewing immediately; if you prefer to manage starting yourself, stop() the panel
     * before calling start() manually.
     */
    public synchronized WebcamPanel getWebcamPanel() {
        if (webcamPanel != null) {
            return webcamPanel;
        }
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(false);
        webcamPanel.setMirrored(false);

        // starting the panel will open the webcam (if not already opened) and start preview thread
        try {
            webcamPanel.start();
        } catch (Throwable t) {
            // panel.start() can throw if native libs missing or camera taken - bubble as status
            notifyStatus("Failed to start webcam preview: " + t.getMessage());
        }
        return webcamPanel;
    }

    /**
     * Start decoding loop in a background thread. Safe to call multiple times.
     */
    public void startScanning() {
        if (running.compareAndSet(false, true)) {
            notifyStatus("Scanning...");
            decoderExecutor.submit(this::decodeLoop);
        }
    }

    /**
     * Stop decoding loop.
     */
    public void stopScanning() {
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
            try { decodedCallback.accept(decoded); } catch (Throwable ignored) {}
        }

        Integer catId = extractCatId(decoded);
        if (catId != null) {
            notifyStatus("Cat ID: " + catId);
            if (catIdCallback != null) {
                try { catIdCallback.accept(catId); } catch (Throwable ignored) {}
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

    private void notifyStatus(String s) {
        if (statusCallback != null) {
            try { statusCallback.accept(s); } catch (Throwable ignored) {}
        }
    }

    /**
     * Stop scanning, stop preview panel thread, shutdown decoder thread and close the webcam.
     * Safe to call multiple times.
     */
    public void dispose() {
        stopScanning();

        try {
            decoderExecutor.shutdownNow();
            if (!decoderExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                decoderExecutor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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

    public Webcam getWebcam() {
        return webcam;
    }
}
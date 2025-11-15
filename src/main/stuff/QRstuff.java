package main.stuff;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

public class QRstuff {

    // Regex patterns to extract cat id (various common formats)
    private static final Pattern CAT_ID_EXACT = Pattern.compile("(?i)^\\s*cat_id\\s*=\\s*(\\d+)\\s*$");
    private static final Pattern CAT_ID_LOOSE = Pattern.compile("(?i).*\\bcat(?:_|-)?id\\b\\s*[:=]?\\s*(\\d+).*");
    private static final Pattern CAT_URL = Pattern.compile("(?i).*/cat/(\\d+).*");
    private static final Pattern GENERIC_ID = Pattern.compile("(?i).*\\b(?:id|cat)\\b\\s*[:=]?\\s*(\\d+).*");

    // debounce time between identical decodes
    private static final long DEBOUNCE_MS = 800L;

    // coarse limits for image resizing for better performance
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;

    private static final List<BarcodeFormat> DEFAULT_FORMATS = Arrays.asList(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.CODE_128
    );

    private final Webcam webcam;
    private WebcamPanel webcamPanel;

    private final ExecutorService decoderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-decoder");
        t.setDaemon(true);
        return t;
    });

    // Executor used to run asynchronous cleanup tasks (dispose) so callers don't have to
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "qr-cleanup");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);

    private String lastDecoded = null;
    private long lastTimeMillis = 0L;

    // Callbacks
    private Consumer<String> decodedCallback;   // receives decoded text
    private Consumer<Result> resultCallback;    // receives full ZXing Result (format + text)
    private Consumer<Integer> catIdCallback;    // receives extracted cat id if present
    private Consumer<String> statusCallback;    // receives human readable status messages

    // Behavior flags
    private volatile boolean autoOpenProfile = true;

    // ZXing decode hints
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
        decodeHints.put(DecodeHintType.POSSIBLE_FORMATS, DEFAULT_FORMATS);

        // choose a reasonable default view size if available
        try {
            if (webcam.getViewSizes() != null && webcam.getViewSizes().length > 0) {
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

        WebcamPanel panel = getWebcamPanel();
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

        parent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension newSize = parent.getSize();
                if (newSize == null) return;
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

        parent.revalidate();
        parent.repaint();
    }

    /**
     * Start decoding loop in a background thread. Safe to call multiple times.
     */
    public void startScanning() {
        if (running.compareAndSet(false, true)) {
            notifyStatus("Scanning...");
            decoderExecutor.submit(this::decodeLoop);
        } else {
            notifyStatus("Scanner already running");
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
        reader.setHints(decodeHints);

        while (running.get()) {
            try {
                BufferedImage frame = webcam.getImage();
                if (frame == null) {
                    Thread.sleep(120);
                    continue;
                }

                // prepare (downscale + contrast) to improve decode reliability/speed
                BufferedImage prepared = prepareFrame(frame);

                // try decode original and small rotations
                Result result = tryDecodeWithRotations(reader, prepared);
                if (result != null) {
                    handleDecodedResult(result);
                    // debounce a bit after successful decode
                    Thread.sleep(Math.max(DEBOUNCE_MS, 300));
                } else {
                    Thread.sleep(120);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                notifyStatus("Decoder loop error: " + t.getMessage());
                // attempt to continue; camera may recover
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        notifyStatus("Scanner stopped.");
    }

    private void handleDecodedResult(Result result) {
        if (result == null) return;
        final String text = result.getText();
        final BarcodeFormat fmt = result.getBarcodeFormat();

        // debounce identical reads
        long now = System.currentTimeMillis();
        boolean isNew = !text.equals(lastDecoded) || (now - lastTimeMillis) > DEBOUNCE_MS;
        if (!isNew) return;
        lastDecoded = text;
        lastTimeMillis = now;

        notifyStatus("Decoded: " + text + " (" + fmt + ")");

        // result callback (format + text)
        if (resultCallback != null) {
            try {
                resultCallback.accept(result);
            } catch (Throwable ignored) {
            }
        }

        // decoded text callback (backwards compat)
        if (decodedCallback != null) {
            try {
                decodedCallback.accept(text);
            } catch (Throwable ignored) {
            }
        }

        // attempt to extract cat id and call callback / possibly open profile
        Integer catId = extractCatId(text);
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
                        main.catProfileMenu profile = new main.catProfileMenu(idToOpen);
                        profile.setVisible(true);
                    } catch (Throwable t) {
                        notifyStatus("Failed to open profile: " + t.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Attempts to decode the provided image, trying the image directly and
     * then a couple of rotated variants to improve robustness for different camera orientations.
     */
    private Result tryDecodeWithRotations(MultiFormatReader reader, BufferedImage img) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            return reader.decodeWithState(bitmap);
        } catch (NotFoundException ignored) {
            // try rotations
        } catch (Throwable e) {
            return null;
        }

        // rotate 90
        BufferedImage r90 = rotateImage(img, 90);
        if (r90 != null) {
            try {
                LuminanceSource source = new BufferedImageLuminanceSource(r90);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                return reader.decodeWithState(bitmap);
            } catch (NotFoundException ignored) {
            } catch (Throwable ignored) {
            }
        }

        // rotate 270
        BufferedImage r270 = rotateImage(img, 270);
        if (r270 != null) {
            try {
                LuminanceSource source = new BufferedImageLuminanceSource(r270);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                return reader.decodeWithState(bitmap);
            } catch (NotFoundException ignored) {
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    /**
     * Downscale large images and apply a cheap contrast stretch to improve
     * decode success on low-contrast webcam captures.
     */
    private BufferedImage prepareFrame(BufferedImage src) {
        BufferedImage scaled = src;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w > MAX_WIDTH || h > MAX_HEIGHT) {
            double scale = Math.min((double) MAX_WIDTH / w, (double) MAX_HEIGHT / h);
            int nw = Math.max(1, (int) (w * scale));
            int nh = Math.max(1, (int) (h * scale));
            Image tmp = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            g.drawImage(tmp, 0, 0, null);
            g.dispose();
            scaled = dst;
        }

        BufferedImage enhanced = contrastStretch(scaled);
        return enhanced;
    }

    private static BufferedImage contrastStretch(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int min = 255, max = 0;
        for (int y = 0; y < h; y += 6) {
            for (int x = 0; x < w; x += 6) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int lum = (r * 30 + g * 59 + b * 11) / 100;
                if (lum < min) min = lum;
                if (lum > max) max = lum;
            }
        }
        int range = Math.max(1, max - min);
        double scale = 255.0 / range;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int lum = (r * 30 + g * 59 + b * 11) / 100;
                int stretched = (int) ((lum - min) * scale);
                int clamped = Math.max(0, Math.min(255, stretched));
                int v = (clamped << 16) | (clamped << 8) | clamped;
                out.setRGB(x, y, v);
            }
        }
        return out;
    }

    private static BufferedImage rotateImage(BufferedImage src, double degrees) {
        if (src == null) return null;
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians)), cos = Math.abs(Math.cos(radians));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);
        BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, newW, newH);
        g.translate((newW - w) / 2, (newH - h) / 2);
        g.rotate(radians, w / 2.0, h / 2.0);
        g.drawRenderedImage(src, null);
        g.dispose();
        return result;
    }

    /**
     * Attempts to extract a cat id from several known patterns. Returns null if none found.
     */
    public static Integer extractCatId(String text) {
        if (text == null) return null;
        Matcher m = CAT_ID_EXACT.matcher(text);
        if (m.matches()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return null; }
        }
        m = CAT_ID_LOOSE.matcher(text);
        if (m.matches()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return null; }
        }
        m = CAT_URL.matcher(text);
        if (m.matches()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return null; }
        }
        m = GENERIC_ID.matcher(text);
        if (m.matches()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    public void setDecodedCallback(Consumer<String> cb) {
        this.decodedCallback = cb;
    }

    public void setResultCallback(Consumer<Result> cb) {
        this.resultCallback = cb;
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
            decoderExecutor.shutdownNow();
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

    /** Convenience overload */
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
package main.stuff;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class QRCodeService {

    private final String baseUrl;
    private final int defaultSize;

    public QRCodeService(String baseUrl) {
        this(baseUrl, 400);
    }

    public QRCodeService(String baseUrl, int sizePx) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.defaultSize = Math.max(200, sizePx);
    }


    public Path generateQRCodeForCat(int catId, String catName, boolean showPreview) throws Exception {
        String payload = buildPayload(catId, catName);
        String filePrefix = safeFileName("cat-" + catId + (catName == null ? "" : "-" + catName));
        Path outPath = buildOutputPath(filePrefix);
        BufferedImage img = renderQRCodeImage(payload, defaultSize);
        writeImageToPathUsingImageIO(img, outPath);
        if (showPreview) {
            SwingUtilities.invokeLater(() -> showImageInWindow(img, "QR for cat " + catId + " — saved to " + outPath.getFileName()));
        }
        return outPath;
    }

    public BufferedImage renderQRCodeForCat(int catId, String catName) throws Exception {
        String payload = buildPayload(catId, catName);
        return renderQRCodeImage(payload, defaultSize);
    }

    public Path saveQRCodeForCat(int catId, String catName, Path outPath) throws Exception {
        BufferedImage img = renderQRCodeForCat(catId, catName);
        writeImageToPathUsingImageIO(img, outPath);
        return outPath;
    }

    private String buildPayload(int catId, String catName) {
        if (!baseUrl.isEmpty()) {
            // Ensure a trailing slash only once
            String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            return url + catId;
        }
        if (catName != null && !catName.trim().isEmpty()) {
            return "cat:" + catName.trim() + " (id:" + catId + ")";
        }
        return "cat:" + catId;
    }

    private BufferedImage renderQRCodeImage(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    // Write using ImageIO for robustness and to avoid re-encoding BitMatrix conversions.
    private void writeImageToPathUsingImageIO(BufferedImage img, Path outPath) throws IOException {
        // Ensure parent exists
        Path parent = outPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException ignored) {
            }
        }

        // Write PNG (overwrites if present)
        ImageIO.write(img, "PNG", outPath.toFile());
    }

    private Path buildOutputPath(String filePrefix) {
        Path downloads = detectDownloadsFolder();
        String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
        String filename = filePrefix + "_" + ts + ".png";
        return downloads.resolve(filename);
    }

    private Path detectDownloadsFolder() {
        // heuristic: $HOME/Downloads, fall back to $HOME
        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isEmpty()) {
                Path p = Paths.get(userHome, "Downloads");
                if (Files.exists(p) && Files.isDirectory(p)) {
                    return p;
                }
                return Paths.get(userHome);
            }
        } catch (Throwable ignored) {
        }
        return Paths.get(System.getProperty("user.dir"));
    }

    private String safeFileName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void showImageInWindow(BufferedImage img, String title) {
        if (img == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame(title);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLabel lbl = new JLabel(new ImageIcon(img));
            lbl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            f.getContentPane().add(lbl, BorderLayout.CENTER);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // Utility used by earlier code; kept for reference but not used by the ImageIO saving approach.
    @SuppressWarnings("unused")
    private BitMatrix convertToBitMatrix(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BitMatrix m = new BitMatrix(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                boolean black = (rgb & 0xFFFFFF) == 0; // crude test (assumes pure black/white)
                if (black) {
                    m.set(x, y);
                }
            }
        }
        return m;
    }
}

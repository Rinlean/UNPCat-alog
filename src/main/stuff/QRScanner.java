package main.stuff;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.zxing.NotFoundException;

public class QRScanner {

    // Regex to extract trailing numeric id from a URL like .../cat/123 or plain "123"
    private static final Pattern ID_EXTRACT = Pattern.compile(".*/(cat|cats)/?(\\d+)$|^(\\d+)$");

    // debounce interval in seconds
    private static final long debounceSeconds = 5;

    public static void main(String[] args) throws Exception {
        // Lazy-load webcam classes so a nicer error is shown if jar missing
        try {
            // get default webcam
            com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (webcam == null) {
                System.err.println("No webcam detected. Ensure a webcam is connected and drivers are available.");
                return;
            }

            // optional: set a reasonable resolution (may be adjusted)
            try {
                com.github.sarxos.webcam.WebcamResolution[] res = com.github.sarxos.webcam.WebcamResolution.values();
                // pick VGA if available
                webcam.setViewSize(com.github.sarxos.webcam.WebcamResolution.VGA.getSize());
            } catch (Throwable t) {
                // ignore resolution set failures
            }

            webcam.open();
            System.out.println("Webcam opened. Point a QR code at the camera. Press Ctrl+C to exit.");

            String lastDecoded = null;
            long lastTimeMillis = 0;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (webcam != null && webcam.isOpen()) {
                        webcam.close();
                        System.out.println("Webcam closed.");
                    }
                } catch (Throwable ignored) {}
            }));

            while (true) {
                BufferedImage image = webcam.getImage();
                if (image == null) {
                    Thread.sleep(100);
                    continue;
                }

                try {
                    String decoded = QRDecoder.decode(image);
                    if (decoded != null) {
                        long now = System.currentTimeMillis();
                        boolean isNew = !decoded.equals(lastDecoded) || (now - lastTimeMillis) > (debounceSeconds * 1000L);
                        if (isNew) {
                            System.out.println("Decoded QR text: " + decoded);
                            Integer catId = extractCatId(decoded);
                            if (catId != null) {
                                System.out.println("Interpreted cat_id = " + catId + " -> fetching from DB...");
                                CatInfoFetcher.fetchAndPrint(catId);
                            } else {
                                System.out.println("Could not parse a numeric cat_id from the QR content.");
                            }
                            lastDecoded = decoded;
                            lastTimeMillis = now;
                        }
                    }
                } catch (NotFoundException nf) {
                    // no QR found in this frame - ignore
                } catch (Throwable t) {
                    System.err.println("Error while decoding frame: " + t.getMessage());
                    t.printStackTrace(System.err);
                }

                Thread.sleep(150); // tune to reduce CPU usage
            }

        } catch (NoClassDefFoundError e) {
            System.err.println("Webcam library (webcam-capture) not found in classpath. Place webcam-capture jar(s) into lib/ to use webcam scanning.");
        }
    }

    /**
     * Extracts a numeric cat id from decoded QR content.
     * Accepts plain integers or URLs ending with /cat/123 or /cats/123.
     */
    private static Integer extractCatId(String text) {
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
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
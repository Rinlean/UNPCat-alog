import main.stuff.QRCodeService;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;

/**
 * Example snippets showing how to use QRCodeService in a UI class that:
 *  - has a cat selector (JComboBox or JList),
 *  - generates a QR when a new cat is added by a caretaker,
 *  - allows generating QR for the currently-selected cat on-demand.
 *
 * This file is illustrative; adapt to your existing UI classes (editCatMenu, adoptMenu, etc.)
 */
public class CatQrIntegrationExample {

    // Example UI components (in your UI class these are real fields)
    private JComboBox<CatItem> catSelector = new JComboBox<>();
    private JButton genQrForSelectedBtn = new JButton("Generate QR for selected");
    private QRCodeService qrService = new QRCodeService("https://yourdomain.example/cat");

    public CatQrIntegrationExample() {
        // wire a button to generate for currently selected cat
        genQrForSelectedBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CatItem sel = (CatItem) catSelector.getSelectedItem();
                if (sel == null || sel.id == 0) {
                    JOptionPane.showMessageDialog(null, "Please select a cat first.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    Path out = qrService.generateQRCodeForCat(sel.id, sel.name, true);
                    JOptionPane.showMessageDialog(null, "QR saved to: " + out.toAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Failed to create QR: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * Call this method after a new cat was inserted into the DB by a caretaker.
     * Example usage inside your "create new cat" logic:
     *
     *   // insert cat -> returns newId
     *   int newId = ...;
     *   String name = ...;
     *   onNewCatAddedByCaretaker(newId, name);
     */
    public void onNewCatAddedByCaretaker(int newCatId, String newCatName) {
        // generate a QR and show preview
        SwingUtilities.invokeLater(() -> {
            try {
                Path out = qrService.generateQRCodeForCat(newCatId, newCatName, true);
                JOptionPane.showMessageDialog(null, "QR for new cat saved to: " + out.toAbsolutePath(), "QR Generated", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Failed to generate QR for new cat: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Simple CatItem used by selector in this example
    public static final class CatItem {
        public final int id;
        public final String name;
        public CatItem(int id, String name) { this.id = id; this.name = name == null ? "" : name; }
        @Override public String toString() { return name; }
    }
}
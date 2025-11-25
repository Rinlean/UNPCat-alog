package main;

import main.stuff.QRCodeService;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Path;

public class viewQRmenu extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(viewQRmenu.class.getName());

    private final Integer accountId;
    private final QRCodeService qrService;

    public viewQRmenu(int accountId) {
        this.accountId = accountId;
        this.qrService = new QRCodeService(""); // pass base URL if you have one
        initComponents();
        // load cats for this caretaker (if accountId is null, list will be empty)
        loadCatsForCaretaker();
    }

    private void loadCatsForCaretaker() {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<CatItem> model = new DefaultListModel<>();

            // If accountId is null -> no one logged in (show placeholder)
            if (accountId == null) {
                model.addElement(new CatItem(0, "<No caretaker logged in>"));
                setListModel(model);
                return;
            }

            boolean isAdmin = (accountId != null && accountId == 0);

            String sql;
            if (isAdmin) {
                // Admin: list all cats
                sql = "SELECT cat_id, name FROM cat ORDER BY name";
                try (Connection conn = main.stuff.dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        model.addElement(new CatItem(rs.getInt("cat_id"), rs.getString("name")));
                    }
                    if (!any) {
                        model.addElement(new CatItem(0, "<No cats found>"));
                    }
                } catch (SQLException ex) {
                    logger.log(java.util.logging.Level.WARNING, "Failed to load cats for admin", ex);
                    model.clear();
                    model.addElement(new CatItem(0, "<Error loading cats>"));
                }
            } else {
                // Regular caretaker: list only cats assigned to this caretaker
                sql = "SELECT c.cat_id, c.name FROM cat c JOIN cat_caretaker cc ON c.cat_id = cc.cat_id WHERE cc.caretaker_id = ? ORDER BY c.name";
                try (Connection conn = main.stuff.dbconn.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean any = false;
                        while (rs.next()) {
                            any = true;
                            model.addElement(new CatItem(rs.getInt("cat_id"), rs.getString("name")));
                        }
                        if (!any) {
                            model.addElement(new CatItem(0, "<No cats assigned>"));
                        }
                    }
                } catch (SQLException ex) {
                    logger.log(java.util.logging.Level.WARNING, "Failed to load cats for caretaker " + accountId, ex);
                    model.clear();
                    model.addElement(new CatItem(0, "<Error loading cats>"));
                }
            }

            setListModel(model);
        });
    }

    private void setListModel(DefaultListModel<CatItem> model) {
        SwingUtilities.invokeLater(() -> {
            @SuppressWarnings("unchecked")
            javax.swing.JList<CatItem> lst = (javax.swing.JList<CatItem>) (Object) listCats;
            lst.setModel(model);
            lst.clearSelection();
        });
    }

    private void showImageDialog(BufferedImage img, String title) {
        if (img == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JDialog dlg = new JDialog(viewQRmenu.this, title, true);
            JLabel lbl = new JLabel(new ImageIcon(img));
            lbl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            dlg.getContentPane().setLayout(new BorderLayout());
            dlg.getContentPane().add(lbl, BorderLayout.CENTER);
            dlg.pack();
            dlg.setLocationRelativeTo(viewQRmenu.this);
            dlg.setVisible(true);
        });
    }

    private static final class CatItem {

        final int id;
        final String name;

        CatItem(int id, String name) {
            this.id = id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ViewQRbtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        listCats = new javax.swing.JList<>();
        SaveImage = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("View QR");
        setMinimumSize(new java.awt.Dimension(422, 350));
        setPreferredSize(new java.awt.Dimension(450, 355));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ViewQRbtn.setText("View Image");
        ViewQRbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewQRbtnActionPerformed(evt);
            }
        });
        getContentPane().add(ViewQRbtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 290, -1, -1));

        listCats.setBorder(javax.swing.BorderFactory.createTitledBorder("List of Cats"));
        listCats.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(listCats);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(23, 20, 280, 240));

        SaveImage.setText("Save Image");
        SaveImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveImageActionPerformed(evt);
            }
        });
        getContentPane().add(SaveImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 250, -1, -1));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void ViewQRbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewQRbtnActionPerformed
        @SuppressWarnings("unchecked")
        javax.swing.JList<CatItem> lst = (javax.swing.JList<CatItem>) (Object) listCats;
        Object sel = lst.getSelectedValue();
        CatItem ci = (sel instanceof CatItem) ? (CatItem) sel : null;
        if (ci == null || ci.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat to view its QR code.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int catId = ci.id;
        final String catName = ci.name;

        new javax.swing.SwingWorker<java.awt.image.BufferedImage, Void>() {
            @Override
            protected java.awt.image.BufferedImage doInBackground() throws Exception {
                try {
                    return qrService.renderQRCodeForCat(catId, catName);
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.WARNING, "Failed to generate QR for cat " + catId, ex);
                    throw ex;
                }
            }

            @Override
            protected void done() {
                try {
                    java.awt.image.BufferedImage img = get();
                    showImageDialog(img, "QR for " + catName + " (id:" + catId + ")");
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.FINE, "QR generation/view failed", ex);
                    JOptionPane.showMessageDialog(viewQRmenu.this, "Failed to generate QR: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }//GEN-LAST:event_ViewQRbtnActionPerformed

    private void SaveImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveImageActionPerformed
        @SuppressWarnings("unchecked")
        javax.swing.JList<CatItem> lst = (javax.swing.JList<CatItem>) (Object) listCats;
        Object sel = lst.getSelectedValue();
        CatItem ci = (sel instanceof CatItem) ? (CatItem) sel : null;
        if (ci == null || ci.id == 0) {
            JOptionPane.showMessageDialog(this, "Please select a cat to save its QR code.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int catId = ci.id;
        final String catName = ci.name;

        // Generate QR in background then prompt user where to save
        new javax.swing.SwingWorker<java.awt.image.BufferedImage, Void>() {
            @Override
            protected java.awt.image.BufferedImage doInBackground() throws Exception {
                try {
                    return qrService.renderQRCodeForCat(catId, catName);
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.WARNING, "Failed to generate QR for cat " + catId, ex);
                    throw ex;
                }
            }

            @Override
            protected void done() {
                java.awt.image.BufferedImage img;
                try {
                    img = get();
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.FINE, "QR generation failed", ex);
                    JOptionPane.showMessageDialog(viewQRmenu.this, "Failed to generate QR: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Build default file name
                String safeName = (catName == null || catName.isEmpty()) ? ("cat-" + catId) : ("cat-" + catId + "-" + catName.replaceAll("[^a-zA-Z0-9._-]", "_"));
                String defaultFileName = safeName + ".png";

                // Show save dialog (default to Downloads or user home)
                javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                chooser.setDialogTitle("Save QR image");
                // set default dir to Downloads if exists
                try {
                    String userHome = System.getProperty("user.home");
                    if (userHome != null && !userHome.isEmpty()) {
                        java.nio.file.Path downloads = java.nio.file.Paths.get(userHome, "Downloads");
                        if (java.nio.file.Files.exists(downloads) && java.nio.file.Files.isDirectory(downloads)) {
                            chooser.setCurrentDirectory(downloads.toFile());
                        } else {
                            chooser.setCurrentDirectory(new java.io.File(userHome));
                        }
                    }
                } catch (Throwable ignored) {
                }
                chooser.setSelectedFile(new java.io.File(defaultFileName));
                int res = chooser.showSaveDialog(viewQRmenu.this);
                if (res != javax.swing.JFileChooser.APPROVE_OPTION) {
                    // user cancelled
                    return;
                }

                java.io.File outFile = chooser.getSelectedFile();
                // Ensure extension .png
                if (!outFile.getName().toLowerCase().endsWith(".png")) {
                    outFile = new java.io.File(outFile.getParentFile(), outFile.getName() + ".png");
                }

                // Use QRCodeService to save the generated image
                try {
                    Path outPath = outFile.toPath();
                    qrService.saveQRCodeForCat(catId, catName, outPath);
                    JOptionPane.showMessageDialog(viewQRmenu.this, "Saved QR image to: " + outPath.toAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.SEVERE, "Failed to save QR image", ex);
                    JOptionPane.showMessageDialog(viewQRmenu.this, "Failed to save QR image: " + ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }//GEN-LAST:event_SaveImageActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton SaveImage;
    private javax.swing.JButton ViewQRbtn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<String> listCats;
    // End of variables declaration//GEN-END:variables
}

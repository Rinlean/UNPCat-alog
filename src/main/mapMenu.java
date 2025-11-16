package main;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import main.stuff.imagerender;

public class mapMenu extends javax.swing.JFrame {

    public mapMenu() {
        initComponents();
        // replace content pane with imagerender background while preserving initComponents() children
        installBackground();
    }
   

    private void installBackground() {
        // change this to the path of your image
        String imagePath = "src\\main\\images\\map.png";

        final imagerender bg = new imagerender(imagePath);
        final JLayeredPane layered = getLayeredPane();
        layered.add(bg, Integer.valueOf(Integer.MIN_VALUE));

        // make sure the content pane is non-opaque so the background is visible
        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setOpaque(false);
        }

        // keep the background sized to the layered pane
        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension s = layered.getSize();
                bg.setBounds(0, 0, s.width, s.height);
                bg.revalidate();
                bg.repaint();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                Dimension s = layered.getSize();
                bg.setBounds(0, 0, s.width, s.height);
                bg.revalidate();
                bg.repaint();
            }
        };

        // attach listeners to frame and layered pane
        addComponentListener(resizeListener);
        layered.addComponentListener(resizeListener);

        // initialize bounds now (will be adjusted again when the window shows/resizes)
        bg.setBounds(0, 0, layered.getWidth(), layered.getHeight());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("UNP Cat-alog Map");
        setMaximumSize(new java.awt.Dimension(1280, 800));
        setMinimumSize(new java.awt.Dimension(1280, 800));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jButton1.setText("jButton1");
        getContentPane().add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(542, 113, -1, -1));

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(623, 113, -1, -1));

        setLocation(new java.awt.Point(600, 300));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}

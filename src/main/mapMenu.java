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

        jComboBox1 = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("UNP Cat-alog");
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setResizable(false);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(631, Short.MAX_VALUE)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(577, 577, 577))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(169, 169, 169)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(609, Short.MAX_VALUE))
        );

        setLocation(new java.awt.Point(600, 300));
    }// </editor-fold>//GEN-END:initComponents

        public static void main(String args[]) {
            try {
                // Set the desired FlatLaf look and feel
                UIManager.setLookAndFeel(new FlatLightLaf()); 

                // Create and display your UI here
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        new mapMenu().setVisible(true);
                    }
                });

            } catch (UnsupportedLookAndFeelException ex) {
            }
        }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBox1;
    // End of variables declaration//GEN-END:variables
}

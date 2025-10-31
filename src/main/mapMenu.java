package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import main.stuff.imagerender;

public class mapMenu extends javax.swing.JFrame {

    public mapMenu() {
        initComponents();
        setExtendedState(mapMenu.MAXIMIZED_BOTH);
        Edit.setVisible(false);
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        mapTab = new javax.swing.JLayeredPane();
        jPanel1 = new javax.swing.JPanel();
        btnCHS = new javax.swing.JToggleButton();
        btnCHS1 = new javax.swing.JToggleButton();
        btnCHS2 = new javax.swing.JToggleButton();
        btnCHS3 = new javax.swing.JToggleButton();
        Edit = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("UNP Cat-alog");
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTabbedPane1.setMinimumSize(new java.awt.Dimension(1280, 720));
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(1280, 720));

        mapTab.setBackground(new java.awt.Color(153, 255, 153));

        jPanel1.setBackground(new java.awt.Color(255, 153, 153));

        btnCHS.setText("CHS");
        btnCHS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCHSActionPerformed(evt);
            }
        });

        btnCHS1.setText("CCJE");
        btnCHS1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCHS1ActionPerformed(evt);
            }
        });

        btnCHS2.setText("Main Library");
        btnCHS2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCHS2ActionPerformed(evt);
            }
        });

        btnCHS3.setText("CHTM");
        btnCHS3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCHS3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnCHS1)
                .addGap(275, 275, 275))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(183, 183, 183)
                        .addComponent(btnCHS))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(248, 248, 248)
                        .addComponent(btnCHS3))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(230, 230, 230)
                        .addComponent(btnCHS2)))
                .addContainerGap(564, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(btnCHS3)
                .addGap(52, 52, 52)
                .addComponent(btnCHS2)
                .addGap(127, 127, 127)
                .addComponent(btnCHS1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCHS)
                .addContainerGap(278, Short.MAX_VALUE))
        );

        mapTab.setLayer(jPanel1, javax.swing.JLayeredPane.DRAG_LAYER);

        javax.swing.GroupLayout mapTabLayout = new javax.swing.GroupLayout(mapTab);
        mapTab.setLayout(mapTabLayout);
        mapTabLayout.setHorizontalGroup(
            mapTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mapTabLayout.setVerticalGroup(
            mapTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Map", mapTab);

        javax.swing.GroupLayout EditLayout = new javax.swing.GroupLayout(Edit);
        Edit.setLayout(EditLayout);
        EditLayout.setHorizontalGroup(
            EditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 890, Short.MAX_VALUE)
        );
        EditLayout.setVerticalGroup(
            EditLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 555, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Edit", Edit);

        getContentPane().add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 890, 590));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCHSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCHSActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCHSActionPerformed

    private void btnCHS1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCHS1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCHS1ActionPerformed

    private void btnCHS2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCHS2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCHS2ActionPerformed

    private void btnCHS3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCHS3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCHS3ActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new mapMenu().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Edit;
    private javax.swing.JToggleButton btnCHS;
    private javax.swing.JToggleButton btnCHS1;
    private javax.swing.JToggleButton btnCHS2;
    private javax.swing.JToggleButton btnCHS3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLayeredPane mapTab;
    // End of variables declaration//GEN-END:variables
}

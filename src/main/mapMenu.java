package main;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import main.stuff.imagerender;
import java.awt.Container;
import javax.swing.JToggleButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class mapMenu extends javax.swing.JFrame {

    private static final String LIGHT_IMAGE = "src\\main\\images\\map.png";
    private static final String DARK_IMAGE = "src\\main\\images\\map_dark.png";

    private imagerender bg;

    public mapMenu() {
        initComponents();
        installBackground();
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            jScrollPane1.setVisible(false);
            jScrollPane2.setVisible(false);
            jScrollPane3.setVisible(false);
            jScrollPane4.setVisible(false);
            jScrollPane5.setVisible(false);
            jScrollPane6.setVisible(false);
            jScrollPane7.setVisible(false);
            jScrollPane8.setVisible(false);
            jScrollPane9.setVisible(false);
            jScrollPane10.setVisible(false);
            jScrollPane11.setVisible(false);
            jScrollPane12.setVisible(false);
            jScrollPane13.setVisible(false);
            jScrollPane14.setVisible(false);
            jScrollPane15.setVisible(false);
            jScrollPane16.setVisible(false);
        });

        populateAllAreaLists();
        registerAllListsForOpenProfile();
    }

    private void installBackground() {
        String imagePath = LIGHT_IMAGE;
        try {
            String lafName = UIManager.getLookAndFeel().getName();
            if (lafName != null && lafName.toLowerCase().contains("dark")) {
                imagePath = DARK_IMAGE;
            }
        } catch (Throwable t) {
        }

        setBackgroundImagePath(imagePath);
    }

    public void setBackgroundImagePath(String imagePath) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setBackgroundImagePath(imagePath));
            return;
        }

        final JLayeredPane layered = getLayeredPane();

        // remove existing bg if present
        try {
            if (bg != null) {
                layered.remove(bg);
            }
        } catch (Throwable ignored) {
        }

        bg = new imagerender(imagePath == null ? LIGHT_IMAGE : imagePath);
        layered.add(bg, Integer.valueOf(Integer.MIN_VALUE));

        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setOpaque(false);
        }

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

        addComponentListener(resizeListener);
        layered.addComponentListener(resizeListener);

        // initialize bounds now
        bg.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        bg.revalidate();
        bg.repaint();

        // force repaint so change is visible immediately
        layered.revalidate();
        layered.repaint();
    }

    public void setDarkModeBackground(boolean dark) {
        setBackgroundImagePath(dark ? DARK_IMAGE : LIGHT_IMAGE);
    }

    private void togglePane(JToggleButton toggle, JScrollPane pane) {
        boolean show = toggle.isSelected();
        pane.setVisible(show);

        Container p = pane.getParent();
        if (p != null) {
            p.revalidate();
            p.repaint();
        } else {
            pane.revalidate();
            pane.repaint();
        }
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

    private void loadCatsIntoListAsync(int areaId, javax.swing.JList<CatItem> list) {
        javax.swing.SwingWorker<java.util.List<CatItem>, Void> worker
                = new javax.swing.SwingWorker<java.util.List<CatItem>, Void>() {
            @Override
            protected java.util.List<CatItem> doInBackground() {
                java.util.List<CatItem> rows = new java.util.ArrayList<>();
                String sql = "SELECT cat_id, name FROM cat WHERE area_id = ? ORDER BY name";
                try (java.sql.Connection conn = main.stuff.dbconn.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, areaId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            rows.add(new CatItem(rs.getInt("cat_id"), rs.getString("name")));
                        }
                    }
                } catch (java.sql.SQLException ex) {
                    java.util.logging.Logger.getLogger(mapMenu.class.getName()).log(java.util.logging.Level.WARNING,
                            "Failed to load cats for area " + areaId, ex);
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    java.util.List<CatItem> listItems = get();
                    javax.swing.DefaultListModel<CatItem> model = new javax.swing.DefaultListModel<>();
                    if (listItems == null || listItems.isEmpty()) {
                        // show a placeholder so users know it's empty
                        model.addElement(new CatItem(0, "<No cats in this area>"));
                    } else {
                        for (CatItem ci : listItems) {
                            model.addElement(ci);
                        }
                    }
                    list.setModel(model);
                } catch (Throwable ex) {
                    java.util.logging.Logger.getLogger(mapMenu.class.getName()).log(java.util.logging.Level.FINE,
                            "Failed to populate JList for area", ex);
                    javax.swing.DefaultListModel<CatItem> model = new javax.swing.DefaultListModel<>();
                    model.addElement(new CatItem(0, "<Error loading cats>"));
                    list.setModel(model);
                }
            }
        };
        worker.execute();
    }

    private void populateAllAreaLists() {
        loadCatsIntoListAsync(1, /* area 1 */ (javax.swing.JList<CatItem>) (Object) jList1);
        loadCatsIntoListAsync(2, /* area 2 */ (javax.swing.JList<CatItem>) (Object) jList2);
        loadCatsIntoListAsync(3, /* area 3 */ (javax.swing.JList<CatItem>) (Object) jList3);
        loadCatsIntoListAsync(4, /* area 4 */ (javax.swing.JList<CatItem>) (Object) jList4);
        loadCatsIntoListAsync(5, /* area 5 */ (javax.swing.JList<CatItem>) (Object) jList5);
        loadCatsIntoListAsync(6, /* area 6 */ (javax.swing.JList<CatItem>) (Object) jList6);
        loadCatsIntoListAsync(7, /* area 7 */ (javax.swing.JList<CatItem>) (Object) jList7);
        loadCatsIntoListAsync(8, /* area 8 */ (javax.swing.JList<CatItem>) (Object) jList8);
        loadCatsIntoListAsync(9, /* area 9 */ (javax.swing.JList<CatItem>) (Object) jList9);
        loadCatsIntoListAsync(10, /* area10 */ (javax.swing.JList<CatItem>) (Object) jList10);
        loadCatsIntoListAsync(11, /* area11 */ (javax.swing.JList<CatItem>) (Object) jList11);
        loadCatsIntoListAsync(12, /* area12 */ (javax.swing.JList<CatItem>) (Object) jList12);
        loadCatsIntoListAsync(13, /* area13 */ (javax.swing.JList<CatItem>) (Object) jList13);
        loadCatsIntoListAsync(14, /* area14 */ (javax.swing.JList<CatItem>) (Object) jList14);
        loadCatsIntoListAsync(15, /* area15 */ (javax.swing.JList<CatItem>) (Object) jList15);
        loadCatsIntoListAsync(16, /* area16 */ (javax.swing.JList<CatItem>) (Object) jList16);
    }

    private void registerListOpenProfile(final javax.swing.JList<?> list) {
        if (list == null) {
            return;
        }

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    Object sel = list.getSelectedValue();
                    if (sel instanceof CatItem) {
                        int catId = ((CatItem) sel).id;
                        if (catId > 0) {
                            openProfileForCat(catId);
                        }
                    }
                }
            }
        });

        list.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    Object sel = list.getSelectedValue();
                    if (sel instanceof CatItem) {
                        int catId = ((CatItem) sel).id;
                        if (catId > 0) {
                            openProfileForCat(catId);
                        }
                    }
                }
            }
        });
    }

    private void registerAllListsForOpenProfile() {
        registerListOpenProfile(jList1);
        registerListOpenProfile(jList2);
        registerListOpenProfile(jList3);
        registerListOpenProfile(jList4);
        registerListOpenProfile(jList5);
        registerListOpenProfile(jList6);
        registerListOpenProfile(jList7);
        registerListOpenProfile(jList8);
        registerListOpenProfile(jList9);
        registerListOpenProfile(jList10);
        registerListOpenProfile(jList11);
        registerListOpenProfile(jList12);
        registerListOpenProfile(jList13);
        registerListOpenProfile(jList14);
        registerListOpenProfile(jList15);
        registerListOpenProfile(jList16);
    }

    private void openProfileForCat(final int catId) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                catProfileMenu prof = new catProfileMenu(catId);
                prof.setVisible(true);
            } catch (Throwable t) {
                java.util.logging.Logger.getLogger(mapMenu.class.getName()).log(java.util.logging.Level.FINE, "Failed to open catProfileMenu", t);
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jToggleButton1 = new javax.swing.JToggleButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList<>();
        jToggleButton2 = new javax.swing.JToggleButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList<>();
        jToggleButton3 = new javax.swing.JToggleButton();
        jToggleButton4 = new javax.swing.JToggleButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList<>();
        jScrollPane5 = new javax.swing.JScrollPane();
        jList5 = new javax.swing.JList<>();
        jToggleButton5 = new javax.swing.JToggleButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        jList6 = new javax.swing.JList<>();
        jToggleButton6 = new javax.swing.JToggleButton();
        jToggleButton7 = new javax.swing.JToggleButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        jList7 = new javax.swing.JList<>();
        jToggleButton8 = new javax.swing.JToggleButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        jList8 = new javax.swing.JList<>();
        jToggleButton9 = new javax.swing.JToggleButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        jList9 = new javax.swing.JList<>();
        jScrollPane10 = new javax.swing.JScrollPane();
        jList10 = new javax.swing.JList<>();
        jToggleButton10 = new javax.swing.JToggleButton();
        jScrollPane11 = new javax.swing.JScrollPane();
        jList11 = new javax.swing.JList<>();
        jToggleButton11 = new javax.swing.JToggleButton();
        jToggleButton12 = new javax.swing.JToggleButton();
        jScrollPane12 = new javax.swing.JScrollPane();
        jList12 = new javax.swing.JList<>();
        jScrollPane13 = new javax.swing.JScrollPane();
        jList13 = new javax.swing.JList<>();
        jToggleButton13 = new javax.swing.JToggleButton();
        jScrollPane14 = new javax.swing.JScrollPane();
        jList14 = new javax.swing.JList<>();
        jToggleButton14 = new javax.swing.JToggleButton();
        jScrollPane15 = new javax.swing.JScrollPane();
        jList15 = new javax.swing.JList<>();
        jToggleButton15 = new javax.swing.JToggleButton();
        jScrollPane16 = new javax.swing.JScrollPane();
        jList16 = new javax.swing.JList<>();
        jToggleButton16 = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("UNP Cat-alog Map");
        setMaximumSize(new java.awt.Dimension(1280, 800));
        setMinimumSize(new java.awt.Dimension(1280, 800));
        setPreferredSize(new java.awt.Dimension(1280, 800));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(790, 150, -1, -1));

        jToggleButton1.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton1.setText("CCIT");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 150, -1, -1));

        jList2.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(jList2);

        getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 210, -1, -1));

        jToggleButton2.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton2.setText("Main Library");
        jToggleButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton2ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 210, -1, -1));

        jList3.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(jList3);

        getContentPane().add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 310, -1, -1));

        jToggleButton3.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton3.setText("Lagoon");
        jToggleButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton3ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 310, -1, -1));

        jToggleButton4.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton4.setText("Gym");
        jToggleButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton4ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton4, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 310, -1, -1));

        jList4.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane4.setViewportView(jList4);

        getContentPane().add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 310, -1, -1));

        jList5.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane5.setViewportView(jList5);

        getContentPane().add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 560, -1, -1));

        jToggleButton5.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton5.setText("Laboratory School");
        jToggleButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton5ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton5, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 530, -1, -1));

        jList6.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane6.setViewportView(jList6);

        getContentPane().add(jScrollPane6, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 350, -1, -1));

        jToggleButton6.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton6.setText("CTE");
        jToggleButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton6ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton6, new org.netbeans.lib.awtextra.AbsoluteConstraints(1040, 390, -1, -1));

        jToggleButton7.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton7.setText("CBAA");
        jToggleButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton7ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton7, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 120, -1, -1));

        jList7.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane7.setViewportView(jList7);

        getContentPane().add(jScrollPane7, new org.netbeans.lib.awtextra.AbsoluteConstraints(1080, 120, -1, -1));

        jToggleButton8.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton8.setText("CTech");
        jToggleButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton8ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton8, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 490, -1, -1));

        jList8.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane8.setViewportView(jList8);

        getContentPane().add(jScrollPane8, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 520, -1, -1));

        jToggleButton9.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton9.setText("CHTM");
        jToggleButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton9ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton9, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 120, -1, -1));

        jList9.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane9.setViewportView(jList9);

        getContentPane().add(jScrollPane9, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 120, -1, -1));

        jList10.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane10.setViewportView(jList10);

        getContentPane().add(jScrollPane10, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 500, -1, -1));

        jToggleButton10.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton10.setText("CArch");
        jToggleButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton10ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton10, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 500, -1, -1));

        jList11.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane11.setViewportView(jList11);

        getContentPane().add(jScrollPane11, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 480, -1, -1));

        jToggleButton11.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton11.setText("CHS");
        jToggleButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton11ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 480, -1, -1));

        jToggleButton12.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton12.setText("CCJE");
        jToggleButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton12ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton12, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 440, -1, -1));

        jList12.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane12.setViewportView(jList12);

        getContentPane().add(jScrollPane12, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 440, -1, -1));

        jList13.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane13.setViewportView(jList13);

        getContentPane().add(jScrollPane13, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 470, -1, -1));

        jToggleButton13.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton13.setText("CFAD");
        jToggleButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton13ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton13, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 470, -1, -1));

        jList14.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane14.setViewportView(jList14);

        getContentPane().add(jScrollPane14, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 400, -1, -1));

        jToggleButton14.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton14.setText("CPAD");
        jToggleButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton14ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton14, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 400, -1, -1));

        jList15.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane15.setViewportView(jList15);

        getContentPane().add(jScrollPane15, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 360, -1, -1));

        jToggleButton15.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton15.setText("COE");
        jToggleButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton15ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton15, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 360, -1, -1));

        jList16.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane16.setViewportView(jList16);

        getContentPane().add(jScrollPane16, new org.netbeans.lib.awtextra.AbsoluteConstraints(920, 180, -1, -1));

        jToggleButton16.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jToggleButton16.setText("CAS");
        jToggleButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton16ActionPerformed(evt);
            }
        });
        getContentPane().add(jToggleButton16, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 230, -1, -1));

        setLocation(new java.awt.Point(400, 200));
    }// </editor-fold>//GEN-END:initComponents

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        togglePane(jToggleButton1, jScrollPane1);
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jToggleButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
        togglePane(jToggleButton2, jScrollPane2);
    }//GEN-LAST:event_jToggleButton2ActionPerformed

    private void jToggleButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton3ActionPerformed
        togglePane(jToggleButton3, jScrollPane3);
    }//GEN-LAST:event_jToggleButton3ActionPerformed

    private void jToggleButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton4ActionPerformed
        togglePane(jToggleButton4, jScrollPane4);
    }//GEN-LAST:event_jToggleButton4ActionPerformed

    private void jToggleButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton5ActionPerformed
        togglePane(jToggleButton5, jScrollPane5);
    }//GEN-LAST:event_jToggleButton5ActionPerformed

    private void jToggleButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton6ActionPerformed
        togglePane(jToggleButton6, jScrollPane6);
    }//GEN-LAST:event_jToggleButton6ActionPerformed

    private void jToggleButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton7ActionPerformed
        togglePane(jToggleButton7, jScrollPane7);
    }//GEN-LAST:event_jToggleButton7ActionPerformed

    private void jToggleButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton8ActionPerformed
        togglePane(jToggleButton8, jScrollPane8);
    }//GEN-LAST:event_jToggleButton8ActionPerformed

    private void jToggleButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton9ActionPerformed
        togglePane(jToggleButton9, jScrollPane9);
    }//GEN-LAST:event_jToggleButton9ActionPerformed

    private void jToggleButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton10ActionPerformed
        togglePane(jToggleButton10, jScrollPane10);
    }//GEN-LAST:event_jToggleButton10ActionPerformed

    private void jToggleButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton11ActionPerformed
        togglePane(jToggleButton11, jScrollPane11);
    }//GEN-LAST:event_jToggleButton11ActionPerformed

    private void jToggleButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton12ActionPerformed
        togglePane(jToggleButton12, jScrollPane12);
    }//GEN-LAST:event_jToggleButton12ActionPerformed

    private void jToggleButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton13ActionPerformed
        togglePane(jToggleButton13, jScrollPane13);
    }//GEN-LAST:event_jToggleButton13ActionPerformed

    private void jToggleButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton14ActionPerformed
        togglePane(jToggleButton14, jScrollPane14);
    }//GEN-LAST:event_jToggleButton14ActionPerformed

    private void jToggleButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton15ActionPerformed
        togglePane(jToggleButton15, jScrollPane15);
    }//GEN-LAST:event_jToggleButton15ActionPerformed

    private void jToggleButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton16ActionPerformed
        togglePane(jToggleButton16, jScrollPane16);
    }//GEN-LAST:event_jToggleButton16ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> jList1;
    private javax.swing.JList<String> jList10;
    private javax.swing.JList<String> jList11;
    private javax.swing.JList<String> jList12;
    private javax.swing.JList<String> jList13;
    private javax.swing.JList<String> jList14;
    private javax.swing.JList<String> jList15;
    private javax.swing.JList<String> jList16;
    private javax.swing.JList<String> jList2;
    private javax.swing.JList<String> jList3;
    private javax.swing.JList<String> jList4;
    private javax.swing.JList<String> jList5;
    private javax.swing.JList<String> jList6;
    private javax.swing.JList<String> jList7;
    private javax.swing.JList<String> jList8;
    private javax.swing.JList<String> jList9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane14;
    private javax.swing.JScrollPane jScrollPane15;
    private javax.swing.JScrollPane jScrollPane16;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JToggleButton jToggleButton10;
    private javax.swing.JToggleButton jToggleButton11;
    private javax.swing.JToggleButton jToggleButton12;
    private javax.swing.JToggleButton jToggleButton13;
    private javax.swing.JToggleButton jToggleButton14;
    private javax.swing.JToggleButton jToggleButton15;
    private javax.swing.JToggleButton jToggleButton16;
    private javax.swing.JToggleButton jToggleButton2;
    private javax.swing.JToggleButton jToggleButton3;
    private javax.swing.JToggleButton jToggleButton4;
    private javax.swing.JToggleButton jToggleButton5;
    private javax.swing.JToggleButton jToggleButton6;
    private javax.swing.JToggleButton jToggleButton7;
    private javax.swing.JToggleButton jToggleButton8;
    private javax.swing.JToggleButton jToggleButton9;
    // End of variables declaration//GEN-END:variables
}

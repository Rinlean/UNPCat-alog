

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Simple JFrame with a left sidebar that you can open/close using setVisible(...)
 * Replace your existing menu.java with this file or copy the relevant parts into
 * your NetBeans form code (see notes below).
 */
public class NewClass extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(NewClass.class.getName());

    // UI components
    private JPanel jPanel1;
    private JPanel jPanelSidebar;
    private JPanel jPanelContent;
    private JButton toggleButton;

    public NewClass() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        // root panel using BorderLayout so hidden sidebar frees space for content
        jPanel1 = new JPanel(new BorderLayout());

        // top bar with toggle button
        toggleButton = new JButton("Toggle Sidebar");
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(toggleButton);

        // sidebar (initially visible) — make preferred width so layout works nicely
        jPanelSidebar = new JPanel();
        jPanelSidebar.setBackground(Color.LIGHT_GRAY);
        jPanelSidebar.setPreferredSize(new Dimension(200, 0));
        jPanelSidebar.setLayout(new BoxLayout(jPanelSidebar, BoxLayout.Y_AXIS));
        jPanelSidebar.add(Box.createVerticalStrut(8));
        jPanelSidebar.add(new JLabel("Sidebar"));
        jPanelSidebar.add(Box.createVerticalStrut(6));
        jPanelSidebar.add(new JButton("Option 1"));
        jPanelSidebar.add(new JButton("Option 2"));
        jPanelSidebar.add(Box.createVerticalGlue());

        // main content area
        jPanelContent = new JPanel();
        jPanelContent.setBackground(Color.WHITE);
        jPanelContent.add(new JLabel("Main content area"));

        // toggle action: use setVisible to show/hide the sidebar, then revalidate/repaint
        toggleButton.addActionListener(e -> {
            boolean nowVisible = !jPanelSidebar.isVisible();
            jPanelSidebar.setVisible(nowVisible);
            // ensure the parent layout recomputes sizes/positions
            jPanel1.revalidate();
            jPanel1.repaint();
            logger.info("Sidebar visible: " + nowVisible);
        });

        // assemble
        jPanel1.add(topBar, BorderLayout.NORTH);
        jPanel1.add(jPanelSidebar, BorderLayout.WEST);
        jPanel1.add(jPanelContent, BorderLayout.CENTER);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(jPanel1, BorderLayout.CENTER);

        // reasonable starting size and centered on screen
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> new NewClass().setVisible(true));
    }
}
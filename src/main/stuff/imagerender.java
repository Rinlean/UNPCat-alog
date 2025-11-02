package main.stuff;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class imagerender extends JPanel {

    private BufferedImage bgimage;

    public imagerender(String image) {
        try {
            bgimage = ImageIO.read(new File(image));
            if (bgimage != null) {
                setPreferredSize(new Dimension(bgimage.getWidth(), bgimage.getHeight()));
            }
        } catch (IOException e) {
            System.err.println("Error loading bg image at: " + image);
            e.printStackTrace();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgimage != null) {
            g.drawImage(bgimage, 0, 0, getWidth(), getHeight(), this);
        }
    }

}

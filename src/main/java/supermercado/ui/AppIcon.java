package supermercado.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class AppIcon {

    private AppIcon() {
        // Utilitario estático
    }

    public static Image getIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(34, 85, 153));
            g.fillRect(0, 0, size, size);

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(8, 12, 14, 8);
            g.drawLine(14, 8, 24, 8);
            g.drawLine(10, 12, 24, 12);
            g.drawLine(12, 12, 8, 20);
            g.drawLine(24, 12, 20, 20);
            g.drawLine(9, 20, 23, 20);
            g.fillOval(10, 22, 4, 4);
            g.fillOval(18, 22, 4, 4);
        } finally {
            g.dispose();
        }
        return image;
    }
}

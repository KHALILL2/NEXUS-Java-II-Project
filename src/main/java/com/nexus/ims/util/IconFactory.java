package com.nexus.ims.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Factory producing crisp, anti-aliased Java2D vector Icons.
 * Eliminates OS/font missing-glyph boxes by rendering geometry directly via Graphics2D.
 */
public final class IconFactory {

    private IconFactory() {}

    @FunctionalInterface
    private interface VectorPainter {
        void paint(Graphics2D g2, Color color);
    }

    private static final class VectorIcon implements Icon {
        private final int width;
        private final int height;
        private final Color color;
        private final VectorPainter painter;

        private VectorIcon(int size, Color color, VectorPainter painter) {
            this(size, size, color, painter);
        }

        private VectorIcon(int width, int height, Color color, VectorPainter painter) {
            this.width = width;
            this.height = height;
            this.color = color;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double scale = height / 16.0;
                g2.scale(scale, scale);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(color);
                painter.paint(g2, color);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    public static Icon brandLogo(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D diamond = new Path2D.Float();
            diamond.moveTo(8, 1.5);
            diamond.lineTo(14.5, 5.2);
            diamond.lineTo(8, 8.9);
            diamond.lineTo(1.5, 5.2);
            diamond.closePath();
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g2.fill(diamond);
            g2.setColor(c);
            g2.draw(diamond);

            Path2D lower = new Path2D.Float();
            lower.moveTo(1.5, 5.2);
            lower.lineTo(1.5, 11.2);
            lower.lineTo(8, 14.8);
            lower.lineTo(14.5, 11.2);
            lower.lineTo(14.5, 5.2);
            g2.draw(lower);
            g2.draw(new Line2D.Float(8, 8.9f, 8, 14.8f));
        });
    }

    public static Icon dashboard(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(2f, 2f, 5f, 5f, 1.5f, 1.5f));
            g2.draw(new RoundRectangle2D.Float(9f, 2f, 5f, 5f, 1.5f, 1.5f));
            g2.draw(new RoundRectangle2D.Float(2f, 9f, 5f, 5f, 1.5f, 1.5f));
            g2.draw(new RoundRectangle2D.Float(9f, 9f, 5f, 5f, 1.5f, 1.5f));
        });
    }

    public static Icon products(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D box = new Path2D.Float();
            box.moveTo(8, 1.8);
            box.lineTo(14, 5);
            box.lineTo(14, 11);
            box.lineTo(8, 14.2);
            box.lineTo(2, 11);
            box.lineTo(2, 5);
            box.closePath();
            g2.draw(box);
            g2.draw(new Line2D.Float(2f, 5f, 8f, 8.2f));
            g2.draw(new Line2D.Float(14f, 5f, 8f, 8.2f));
            g2.draw(new Line2D.Float(8f, 8.2f, 8f, 14.2f));
        });
    }

    public static Icon transactions(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            // Top right arrow
            g2.draw(new Line2D.Float(2.5f, 5.2f, 13.5f, 5.2f));
            g2.draw(new Line2D.Float(10.8f, 2.7f, 13.5f, 5.2f));
            g2.draw(new Line2D.Float(10.8f, 7.7f, 13.5f, 5.2f));
            // Bottom left arrow
            g2.draw(new Line2D.Float(13.5f, 10.8f, 2.5f, 10.8f));
            g2.draw(new Line2D.Float(5.2f, 8.3f, 2.5f, 10.8f));
            g2.draw(new Line2D.Float(5.2f, 13.3f, 2.5f, 10.8f));
        });
    }

    public static Icon suppliers(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(2.5f, 3f, 11f, 11f, 1.2f, 1.2f));
            g2.draw(new Line2D.Float(1.5f, 14f, 14.5f, 14f));
            g2.draw(new Line2D.Float(5.5f, 5.8f, 6.8f, 5.8f));
            g2.draw(new Line2D.Float(9.2f, 5.8f, 10.5f, 5.8f));
            g2.draw(new Line2D.Float(5.5f, 8.5f, 6.8f, 8.5f));
            g2.draw(new Line2D.Float(9.2f, 8.5f, 10.5f, 8.5f));
            g2.draw(new Rectangle2D.Float(6.8f, 11f, 2.4f, 3f));
        });
    }

    public static Icon reports(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Line2D.Float(2f, 13.5f, 14f, 13.5f));
            g2.draw(new RoundRectangle2D.Float(3f, 9f, 2.2f, 4.5f, 0.8f, 0.8f));
            g2.draw(new RoundRectangle2D.Float(6.9f, 6.2f, 2.2f, 7.3f, 0.8f, 0.8f));
            g2.draw(new RoundRectangle2D.Float(10.8f, 3.2f, 2.2f, 10.3f, 0.8f, 0.8f));
        });
    }

    public static Icon signOut(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D door = new Path2D.Float();
            door.moveTo(6.5, 2.5);
            door.lineTo(3, 2.5);
            door.lineTo(3, 13.5);
            door.lineTo(6.5, 13.5);
            g2.draw(door);
            g2.draw(new Line2D.Float(6.5f, 8f, 13.5f, 8f));
            g2.draw(new Line2D.Float(10.8f, 5.2f, 13.5f, 8f));
            g2.draw(new Line2D.Float(10.8f, 10.8f, 13.5f, 8f));
        });
    }

    public static Icon add(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Line2D.Float(8f, 3f, 8f, 13f));
            g2.draw(new Line2D.Float(3f, 8f, 13f, 8f));
        });
    }

    public static Icon edit(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D pencil = new Path2D.Float();
            pencil.moveTo(10.8, 2.5);
            pencil.lineTo(13.5, 5.2);
            pencil.lineTo(5.6, 13.1);
            pencil.lineTo(2.4, 13.6);
            pencil.lineTo(2.9, 10.4);
            pencil.closePath();
            g2.draw(pencil);
            g2.draw(new Line2D.Float(9.2f, 4.1f, 11.9f, 6.8f));
        });
    }

    public static Icon delete(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Line2D.Float(2.5f, 4.2f, 13.5f, 4.2f));
            g2.draw(new RoundRectangle2D.Float(5.8f, 2f, 4.4f, 2.2f, 0.8f, 0.8f));
            Path2D bin = new Path2D.Float();
            bin.moveTo(3.8, 4.2);
            bin.lineTo(4.6, 13.6);
            bin.lineTo(11.4, 13.6);
            bin.lineTo(12.2, 4.2);
            g2.draw(bin);
            g2.draw(new Line2D.Float(6.6f, 6.8f, 6.8f, 11.2f));
            g2.draw(new Line2D.Float(9.4f, 6.8f, 9.2f, 11.2f));
        });
    }

    public static Icon refresh(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Arc2D.Float(2.5f, 2.5f, 11f, 11f, 35f, 285f, Arc2D.OPEN));
            g2.draw(new Line2D.Float(10.8f, 2.5f, 13.4f, 4.6f));
            g2.draw(new Line2D.Float(13.4f, 4.6f, 10.6f, 5.6f));
        });
    }

    public static Icon stockIn(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Line2D.Float(8f, 2.2f, 8f, 9.8f));
            g2.draw(new Line2D.Float(5.3f, 7.2f, 8f, 9.8f));
            g2.draw(new Line2D.Float(10.7f, 7.2f, 8f, 9.8f));
            Path2D tray = new Path2D.Float();
            tray.moveTo(2.2, 10.2);
            tray.lineTo(2.2, 13.6);
            tray.lineTo(13.8, 13.6);
            tray.lineTo(13.8, 10.2);
            g2.draw(tray);
        });
    }

    public static Icon stockOut(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Line2D.Float(8f, 9.8f, 8f, 2.2f));
            g2.draw(new Line2D.Float(5.3f, 4.8f, 8f, 2.2f));
            g2.draw(new Line2D.Float(10.7f, 4.8f, 8f, 2.2f));
            Path2D tray = new Path2D.Float();
            tray.moveTo(2.2, 10.2);
            tray.lineTo(2.2, 13.6);
            tray.lineTo(13.8, 13.6);
            tray.lineTo(13.8, 10.2);
            g2.draw(tray);
        });
    }

    public static Icon search(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Ellipse2D.Float(2.4f, 2.4f, 8.4f, 8.4f));
            g2.draw(new Line2D.Float(9.8f, 9.8f, 13.6f, 13.6f));
        });
    }

    public static Icon export(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D doc = new Path2D.Float();
            doc.moveTo(3.2, 2.0);
            doc.lineTo(10.2, 2.0);
            doc.lineTo(12.8, 4.6);
            doc.lineTo(12.8, 14.0);
            doc.lineTo(3.2, 14.0);
            doc.closePath();
            g2.draw(doc);
            g2.draw(new Line2D.Float(8f, 6.2f, 8f, 11.4f));
            g2.draw(new Line2D.Float(5.8f, 9.4f, 8f, 11.4f));
            g2.draw(new Line2D.Float(10.2f, 9.4f, 8f, 11.4f));
        });
    }

    public static Icon alert(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D tri = new Path2D.Float();
            tri.moveTo(8, 2.0);
            tri.lineTo(14.3, 13.5);
            tri.lineTo(1.7, 13.5);
            tri.closePath();
            g2.draw(tri);
            g2.draw(new Line2D.Float(8f, 6.2f, 8f, 9.5f));
            g2.fill(new Ellipse2D.Float(7.25f, 11.0f, 1.5f, 1.5f));
        });
    }

    public static Icon statusDot(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.fill(new Ellipse2D.Float(3f, 3f, 10f, 10f));
        });
    }

    public static Icon exportDropdown(int size, Color color) {
        int width = Math.round(size * 1.55f);
        return new VectorIcon(width, size, color, (g2, c) -> {
            // Document export icon on left (0..14)
            Path2D doc = new Path2D.Float();
            doc.moveTo(2.4, 2.0);
            doc.lineTo(9.2, 2.0);
            doc.lineTo(11.8, 4.6);
            doc.lineTo(11.8, 14.0);
            doc.lineTo(2.4, 14.0);
            doc.closePath();
            g2.draw(doc);
            g2.draw(new Line2D.Float(7.1f, 6.2f, 7.1f, 11.3f));
            g2.draw(new Line2D.Float(5.0f, 9.3f, 7.1f, 11.3f));
            g2.draw(new Line2D.Float(9.2f, 9.3f, 7.1f, 11.3f));

            // Dropdown chevron triangle on right (16.5..22.5)
            Path2D chevron = new Path2D.Float();
            chevron.moveTo(16.2, 6.4);
            chevron.lineTo(22.6, 6.4);
            chevron.lineTo(19.4, 10.6);
            chevron.closePath();
            g2.fill(chevron);
        });
    }

    public static Icon pdfFile(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D page = new Path2D.Float();
            page.moveTo(3.0, 1.8);
            page.lineTo(10.0, 1.8);
            page.lineTo(13.0, 4.8);
            page.lineTo(13.0, 14.2);
            page.lineTo(3.0, 14.2);
            page.closePath();
            g2.draw(page);
            g2.draw(new Line2D.Float(10.0f, 1.8f, 10.0f, 4.8f));
            g2.draw(new Line2D.Float(10.0f, 4.8f, 13.0f, 4.8f));
            g2.draw(new Line2D.Float(5.2f, 7.6f, 10.8f, 7.6f));
            g2.draw(new Line2D.Float(5.2f, 10.0f, 10.8f, 10.0f));
            g2.draw(new Line2D.Float(5.2f, 12.2f, 8.6f, 12.2f));
        });
    }

    public static Icon excelFile(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(2.0f, 2.2f, 12.0f, 11.6f, 1.4f, 1.4f));
            g2.draw(new Line2D.Float(2.0f, 5.8f, 14.0f, 5.8f));
            g2.draw(new Line2D.Float(2.0f, 9.8f, 14.0f, 9.8f));
            g2.draw(new Line2D.Float(6.8f, 2.2f, 6.8f, 13.8f));
        });
    }

    public static Icon csvFile(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(2.0f, 2.2f, 12.0f, 11.6f, 1.4f, 1.4f));
            g2.draw(new Line2D.Float(4.2f, 5.6f, 6.8f, 5.6f));
            g2.draw(new Line2D.Float(9.2f, 5.6f, 11.8f, 5.6f));
            g2.draw(new Line2D.Float(4.2f, 8.2f, 6.8f, 8.2f));
            g2.draw(new Line2D.Float(9.2f, 8.2f, 11.8f, 8.2f));
            g2.draw(new Line2D.Float(4.2f, 10.8f, 6.8f, 10.8f));
            g2.draw(new Line2D.Float(9.2f, 10.8f, 11.8f, 10.8f));
        });
    }

    public static Icon jsonFile(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D leftBrace = new Path2D.Float();
            leftBrace.moveTo(6.2, 2.5);
            leftBrace.lineTo(4.4, 2.5);
            leftBrace.lineTo(4.4, 6.6);
            leftBrace.lineTo(2.6, 8.0);
            leftBrace.lineTo(4.4, 9.4);
            leftBrace.lineTo(4.4, 13.5);
            leftBrace.lineTo(6.2, 13.5);
            g2.draw(leftBrace);

            Path2D rightBrace = new Path2D.Float();
            rightBrace.moveTo(9.8, 2.5);
            rightBrace.lineTo(11.6, 2.5);
            rightBrace.lineTo(11.6, 6.6);
            rightBrace.lineTo(13.4, 8.0);
            rightBrace.lineTo(11.6, 9.4);
            rightBrace.lineTo(11.6, 13.5);
            rightBrace.lineTo(9.8, 13.5);
            g2.draw(rightBrace);
        });
    }
}

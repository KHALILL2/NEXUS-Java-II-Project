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

    public static Icon users(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            // Primary user head & shoulders
            g2.draw(new Ellipse2D.Float(3.8f, 2.6f, 4.4f, 4.4f));
            Path2D body1 = new Path2D.Float();
            body1.moveTo(1.8, 13.6);
            body1.quadTo(1.8, 9.4, 6.0, 9.4);
            body1.quadTo(10.2, 9.4, 10.2, 13.6);
            g2.draw(body1);

            // Secondary user head & shoulder arc
            g2.draw(new Ellipse2D.Float(9.6f, 3.6f, 3.4f, 3.4f));
            Path2D body2 = new Path2D.Float();
            body2.moveTo(11.2, 9.5);
            body2.quadTo(14.2, 9.7, 14.4, 13.4);
            g2.draw(body2);
        });
    }

    public static Icon key(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Ellipse2D.Float(2.0f, 5.2f, 5.2f, 5.2f));
            g2.draw(new Line2D.Float(7.2f, 7.8f, 14.0f, 7.8f));
            g2.draw(new Line2D.Float(11.4f, 7.8f, 11.4f, 10.6f));
            g2.draw(new Line2D.Float(13.8f, 7.8f, 13.8f, 11.0f));
        });
    }

    public static Icon categories(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D tag = new Path2D.Float();
            tag.moveTo(2.2, 2.4);
            tag.lineTo(8.2, 2.4);
            tag.lineTo(13.8, 8.0);
            tag.lineTo(8.0, 13.8);
            tag.lineTo(2.2, 8.2);
            tag.closePath();
            g2.draw(tag);
            g2.fill(new Ellipse2D.Float(4.4f, 4.6f, 1.8f, 1.8f));
        });
    }

    /**
     * Sun icon for Light Theme mode.
     */
    public static Icon sun(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Ellipse2D.Float(5.0f, 5.0f, 6.0f, 6.0f));
            g2.draw(new Line2D.Float(8.0f, 1.5f, 8.0f, 3.2f));
            g2.draw(new Line2D.Float(8.0f, 12.8f, 8.0f, 14.5f));
            g2.draw(new Line2D.Float(1.5f, 8.0f, 3.2f, 8.0f));
            g2.draw(new Line2D.Float(12.8f, 8.0f, 14.5f, 8.0f));
            g2.draw(new Line2D.Float(3.4f, 3.4f, 4.6f, 4.6f));
            g2.draw(new Line2D.Float(11.4f, 11.4f, 12.6f, 12.6f));
            g2.draw(new Line2D.Float(3.4f, 12.6f, 4.6f, 11.4f));
            g2.draw(new Line2D.Float(11.4f, 4.6f, 12.6f, 3.4f));
        });
    }

    /**
     * Crescent moon icon for Dark Theme mode.
     */
    public static Icon moon(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D moon = new Path2D.Float();
            moon.moveTo(10.6, 2.2);
            moon.curveTo(6.4, 2.6, 3.4, 6.4, 4.2, 10.4);
            moon.curveTo(4.9, 13.6, 8.4, 15.0, 11.8, 13.4);
            moon.curveTo(12.8, 12.9, 13.5, 12.2, 13.9, 11.3);
            moon.curveTo(11.0, 12.0, 8.1, 10.2, 7.6, 7.3);
            moon.curveTo(7.2, 5.1, 8.4, 3.1, 10.6, 2.2);
            moon.closePath();
            g2.draw(moon);
        });
    }

    /**
     * Monitor/contrast icon for System Default Theme mode.
     */
    public static Icon systemTheme(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(1.8f, 2.5f, 12.4f, 8.8f, 1.8f, 1.8f));
            g2.draw(new Line2D.Float(5.2f, 13.8f, 10.8f, 13.8f));
            g2.draw(new Line2D.Float(8.0f, 11.3f, 8.0f, 13.8f));
            Path2D half = new Path2D.Float();
            half.moveTo(8.0, 4.0);
            half.lineTo(12.6, 4.0);
            half.lineTo(12.6, 9.8);
            half.lineTo(8.0, 9.8);
            half.closePath();
            g2.fill(half);
        });
    }

    /**
     * Globe icon for Language selector.
     */
    public static Icon globe(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new Ellipse2D.Float(2.0f, 2.0f, 12.0f, 12.0f));
            g2.draw(new Ellipse2D.Float(5.2f, 2.0f, 5.6f, 12.0f));
            g2.draw(new Line2D.Float(2.2f, 8.0f, 13.8f, 8.0f));
            g2.draw(new Line2D.Float(3.4f, 5.0f, 12.6f, 5.0f));
            g2.draw(new Line2D.Float(3.4f, 11.0f, 12.6f, 11.0f));
        });
    }

    /**
     * Stacked warehouse boxes icon for total stocked units KPI.
     */
    public static Icon warehouseBoxes(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(4.8f, 2.0f, 6.4f, 5.2f, 1.2f, 1.2f));
            g2.draw(new Line2D.Float(8.0f, 2.0f, 8.0f, 3.8f));

            g2.draw(new RoundRectangle2D.Float(1.8f, 8.6f, 5.6f, 5.4f, 1.2f, 1.2f));
            g2.draw(new Line2D.Float(4.6f, 8.6f, 4.6f, 10.4f));

            g2.draw(new RoundRectangle2D.Float(8.6f, 8.6f, 5.6f, 5.4f, 1.2f, 1.2f));
            g2.draw(new Line2D.Float(11.4f, 8.6f, 11.4f, 10.4f));
        });
    }

    /**
     * Wallet currency badge icon for inventory valuation KPI.
     */
    public static Icon dollarWallet(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(1.8f, 3.0f, 12.4f, 10.0f, 2.2f, 2.2f));
            g2.draw(new RoundRectangle2D.Float(9.8f, 6.3f, 4.4f, 3.4f, 1.2f, 1.2f));
            g2.fill(new Ellipse2D.Float(11.0f, 7.3f, 1.4f, 1.4f));

            g2.draw(new Line2D.Float(5.8f, 4.8f, 5.8f, 11.2f));
            Path2D s = new Path2D.Float();
            s.moveTo(7.4, 5.7);
            s.curveTo(5.8, 5.1, 4.2, 5.7, 4.4, 7.0);
            s.curveTo(4.6, 8.1, 7.2, 7.9, 7.2, 9.2);
            s.curveTo(7.2, 10.4, 5.4, 10.8, 4.2, 10.1);
            g2.draw(s);
        });
    }

    /**
     * Sidebar layout toggle icon.
     */
    public static Icon sidebarToggle(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(1.8f, 2.4f, 12.4f, 11.2f, 2.0f, 2.0f));
            g2.draw(new Line2D.Float(5.8f, 2.4f, 5.8f, 13.6f));
            g2.draw(new Line2D.Float(3.3f, 5.3f, 4.4f, 5.3f));
            g2.draw(new Line2D.Float(3.3f, 7.8f, 4.4f, 7.8f));
            g2.draw(new Line2D.Float(7.8f, 6.0f, 12.2f, 6.0f));
            g2.draw(new Line2D.Float(7.8f, 9.2f, 10.8f, 9.2f));
        });
    }

    /**
     * Verified shield checkmark icon for healthy stock empty states.
     */
    public static Icon checkShield(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            Path2D shield = new Path2D.Float();
            shield.moveTo(8.0, 1.7);
            shield.lineTo(13.3, 3.7);
            shield.curveTo(13.3, 8.4, 12.3, 11.6, 8.0, 14.3);
            shield.curveTo(3.7, 11.6, 2.7, 8.4, 2.7, 3.7);
            shield.closePath();
            g2.draw(shield);

            Path2D check = new Path2D.Float();
            check.moveTo(5.4, 8.0);
            check.lineTo(7.2, 9.9);
            check.lineTo(10.8, 6.0);
            g2.draw(check);
        });
    }

    /**
     * Copy-to-clipboard icon for SKU context menu action.
     */
    public static Icon copy(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(2.4f, 5.0f, 8.2f, 8.8f, 1.6f, 1.6f));
            Path2D back = new Path2D.Float();
            back.moveTo(5.6, 4.8);
            back.lineTo(5.6, 2.5);
            back.lineTo(13.4, 2.5);
            back.lineTo(13.4, 11.0);
            back.lineTo(10.8, 11.0);
            g2.draw(back);
        });
    }

    /**
     * Calendar icon for timeframe filtering.
     */
    public static Icon calendar(int size, Color color) {
        return new VectorIcon(size, color, (g2, c) -> {
            g2.draw(new RoundRectangle2D.Float(2.0f, 3.2f, 12.0f, 10.8f, 1.8f, 1.8f));
            g2.draw(new Line2D.Float(2.0f, 6.6f, 14.0f, 6.6f));
            g2.draw(new Line2D.Float(5.2f, 1.7f, 5.2f, 4.2f));
            g2.draw(new Line2D.Float(10.8f, 1.7f, 10.8f, 4.2f));
            g2.fill(new Ellipse2D.Float(4.3f, 8.3f, 1.6f, 1.6f));
            g2.fill(new Ellipse2D.Float(7.2f, 8.3f, 1.6f, 1.6f));
            g2.fill(new Ellipse2D.Float(10.1f, 8.3f, 1.6f, 1.6f));
            g2.fill(new Ellipse2D.Float(4.3f, 10.9f, 1.6f, 1.6f));
        });
    }
}

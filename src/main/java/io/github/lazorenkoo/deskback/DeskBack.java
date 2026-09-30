package io.github.lazorenkoo.deskback;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Path2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DeskBack {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(DeskBack::showWindow);
    }

    private static void showWindow() {
        JFrame window = new JFrame("DeskBack");
        window.setUndecorated(true);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setResizable(false);
        window.setAlwaysOnTop(true);
        window.setAutoRequestFocus(false);
        window.setType(JFrame.Type.UTILITY);

        JButton restoreButton = new JButton("Restore") {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color background = new Color(237, 238, 234);
                if (getModel().isPressed()) {
                    background = new Color(218, 224, 215);
                } else if (getModel().isRollover()) {
                    background = new Color(228, 233, 225);
                }
                canvas.setColor(background);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                canvas.translate(-3, -6);

                canvas.setColor(new Color(220, 226, 216));
                canvas.fillRoundRect(13, 15, 30, 30, 12, 12);
                canvas.setColor(new Color(99, 119, 93));
                canvas.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(21, 26);
                arrow.lineTo(29, 26);
                arrow.curveTo(37, 26, 37, 36, 29, 36);
                arrow.lineTo(26, 36);
                canvas.draw(arrow);
                canvas.drawLine(25, 22, 21, 26);
                canvas.drawLine(21, 26, 25, 30);
                canvas.dispose();
                super.paintComponent(graphics);
            }
        };
        restoreButton.setPreferredSize(new Dimension(160, 48));
        restoreButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        restoreButton.setForeground(new Color(64, 73, 60));
        restoreButton.setContentAreaFilled(false);
        restoreButton.setOpaque(false);
        restoreButton.setFocusPainted(false);
        restoreButton.setBorderPainted(false);
        restoreButton.setRolloverEnabled(true);
        restoreButton.setBorder(BorderFactory.createEmptyBorder(0, 44, 0, 10));
        restoreButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        restoreButton.addActionListener(event -> {
            restoreButton.setText("Nothing saved");
            restoreButton.setCursor(Cursor.getDefaultCursor());
        });

        window.setBackground(new Color(237, 238, 234));
        if (window.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            window.setBackground(new Color(0, 0, 0, 0));
        }
        window.getRootPane().setOpaque(false);
        window.getLayeredPane().setOpaque(false);
        window.getRootPane().setBorder(BorderFactory.createEmptyBorder());
        window.setContentPane(restoreButton);
        window.getRootPane().setDefaultButton(restoreButton);
        window.pack();

        GraphicsConfiguration screen = window.getGraphicsConfiguration();
        Rectangle bounds = screen.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        int x = bounds.x + bounds.width - insets.right - window.getWidth() - 16;
        int y = bounds.y + bounds.height - insets.bottom - window.getHeight() - 16;
        window.setLocation(x, y);
        window.setVisible(true);

        Timer closeTimer = new Timer(10_000, event -> window.dispose());
        closeTimer.setRepeats(false);
        closeTimer.start();
    }
}

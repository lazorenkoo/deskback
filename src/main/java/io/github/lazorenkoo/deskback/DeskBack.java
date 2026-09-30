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
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DeskBack {
    public static void main(String[] args) {
        boolean darkTheme = isWindowsDarkTheme();
        SwingUtilities.invokeLater(() -> showWindow(darkTheme));
    }

    private static boolean isWindowsDarkTheme() {
        Process process = null;
        try {
            process = new ProcessBuilder("reg.exe", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme").redirectErrorStream(true).start();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return process.exitValue() == 0
                    && output.lines().anyMatch(line -> line.trim().matches(
                            "(?i)AppsUseLightTheme\\s+REG_DWORD\\s+0x0+"));
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void showWindow(boolean darkTheme) {
        Color normalColor = darkTheme ? new Color(40, 43, 40) : new Color(237, 238, 234);
        Color hoverColor = darkTheme ? new Color(51, 57, 50) : new Color(228, 233, 225);
        Color pressedColor = darkTheme ? new Color(60, 68, 58) : new Color(218, 224, 215);
        Color textColor = darkTheme ? new Color(250, 250, 250) : new Color(64, 73, 60);
        Color iconBackground = darkTheme ? new Color(57, 66, 54) : new Color(220, 226, 216);
        Color iconColor = darkTheme ? new Color(175, 196, 162) : new Color(99, 119, 93);
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
                Color background = normalColor;
                if (getModel().isPressed()) {
                    background = pressedColor;
                } else if (getModel().isRollover()) {
                    background = hoverColor;
                }
                canvas.setColor(background);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                canvas.translate(-3, -6);

                canvas.setColor(iconBackground);
                canvas.fillRoundRect(13, 15, 30, 30, 12, 12);
                canvas.setColor(iconColor);
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
        restoreButton.setFont(new Font("Segoe UI", darkTheme ? Font.BOLD : Font.PLAIN, 14)
                .deriveFont(Map.of(TextAttribute.TRACKING, 0.06f)));
        restoreButton.setForeground(textColor);
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

        window.setBackground(normalColor);
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

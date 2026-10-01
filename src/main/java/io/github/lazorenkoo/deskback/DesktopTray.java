package io.github.lazorenkoo.deskback;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

final class DesktopTray {
    private static TrayIcon icon;

    static void start(Runnable restore) {
        if (!SystemTray.isSupported()) {
            throw new IllegalStateException("Windows notification area is unavailable.");
        }
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = image.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(new Color(175, 196, 162));
        canvas.fillRoundRect(1, 1, 30, 30, 12, 12);
        canvas.setColor(new Color(40, 43, 40));
        canvas.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.drawArc(9, 10, 15, 14, -90, 270);
        canvas.drawLine(9, 10, 9, 16);
        canvas.drawLine(9, 10, 15, 10);
        canvas.dispose();
        PopupMenu menu = new PopupMenu();
        MenuItem restoreItem = new MenuItem("Restore");
        restoreItem.addActionListener(event -> SwingUtilities.invokeLater(restore));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(event -> {
            SystemTray.getSystemTray().remove(icon);
            System.exit(0);
        });
        menu.add(restoreItem);
        menu.add(exitItem);
        icon = new TrayIcon(image, "DeskBack", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(event -> SwingUtilities.invokeLater(restore));
        try {
            SystemTray.getSystemTray().add(icon);
        } catch (AWTException exception) {
            throw new IllegalStateException("Could not add DeskBack to the notification area.", exception);
        }
    }

    static void showError(String message) {
        SwingUtilities.invokeLater(() -> icon.displayMessage("DeskBack", message, TrayIcon.MessageType.ERROR));
    }
}

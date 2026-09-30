package io.github.lazorenkoo.deskback;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;

final class AppSelection {
    static void show(JFrame window, boolean darkTheme, List<WindowsApps.RunningApp> runningApps) {
        Color background = darkTheme ? new Color(30, 33, 31) : new Color(244, 245, 242);
        Color foreground = darkTheme ? new Color(245, 247, 244) : new Color(35, 43, 34);
        Color selected = darkTheme ? new Color(49, 77, 53) : new Color(217, 235, 212);
        Color excluded = darkTheme ? new Color(44, 47, 45) : new Color(231, 233, 228);
        Color accent = darkTheme ? new Color(180, 209, 169) : new Color(57, 91, 49);

        JPanel panel = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                canvas.setColor(background);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                canvas.dispose();
                super.paintComponent(graphics);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<JToggleButton> apps = new ArrayList<>();
        JButton restore = new JButton("Restore (" + runningApps.size() + ")") {
            @Override
            protected void paintComponent(Graphics graphics) {
                paintButton(graphics, this, isEnabled() ? accent : excluded,
                        isEnabled() ? background : foreground, false);
            }
        };
        styleButton(restore);
        restore.setEnabled(!runningApps.isEmpty());
        restore.setToolTipText("Launching apps will be added in the next step.");
        restore.addActionListener(event -> restore.setText("Not ready yet"));

        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 6));
        rows.setBackground(background);
        for (WindowsApps.RunningApp runningApp : runningApps) {
            JToggleButton app = new JToggleButton(runningApp.name(), true) {
                @Override
                protected void paintComponent(Graphics graphics) {
                    paintButton(graphics, this, isSelected() ? selected : excluded, foreground, true);
                }
            };
            styleButton(app);
            app.setToolTipText(runningApp.executablePath().toString());
            app.putClientProperty("runningApp", runningApp);
            app.setPreferredSize(new Dimension(240, 36));
            apps.add(app);
            app.addItemListener(event -> {
                long count = apps.stream().filter(JToggleButton::isSelected).count();
                restore.setText("Restore (" + count + ")");
                restore.setEnabled(count > 0);
            });
            rows.add(app);
        }
        if (runningApps.isEmpty()) {
            JLabel empty = new JLabel("No apps found");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            empty.setForeground(foreground);
            rows.add(empty);
        }

        JScrollPane scroll = new JScrollPane(rows, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(background);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        panel.add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close") {
            @Override
            protected void paintComponent(Graphics graphics) {
                paintButton(graphics, this, excluded, foreground, false);
            }
        };
        styleButton(close);
        close.addActionListener(event -> window.dispose());
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(240, 34));
        actions.add(close);
        actions.add(restore);
        panel.add(actions, BorderLayout.SOUTH);

        Rectangle bounds = window.getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(window.getGraphicsConfiguration());
        int availableHeight = bounds.height - insets.top - insets.bottom - 32;
        int contentHeight = Math.max(36, runningApps.size() * 42 - 6) + 64;
        panel.setPreferredSize(new Dimension(260, Math.min(contentHeight, availableHeight)));
        window.setContentPane(panel);
        window.getRootPane().setDefaultButton(restore);
        window.pack();
        window.setLocation(bounds.x + bounds.width - insets.right - window.getWidth() - 16,
                bounds.y + bounds.height - insets.bottom - window.getHeight() - 16);
        window.revalidate();
        window.repaint();
    }

    private static void styleButton(AbstractButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static void paintButton(Graphics graphics, AbstractButton button, Color background,
                                    Color foreground, boolean leftAligned) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setColor(background);
        canvas.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 14, 14);
        if (button.isEnabled() && (button.getModel().isRollover() || button.hasFocus())) {
            canvas.setColor(new Color(127, 127, 127, 24));
            canvas.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 14, 14);
        }
        canvas.setFont(button.getFont());
        FontMetrics metrics = canvas.getFontMetrics();
        String text = button.getText();
        int maxWidth = Math.max(0, button.getWidth() - 28);
        if (metrics.stringWidth(text) > maxWidth) {
            while (!text.isEmpty() && metrics.stringWidth(text + "…") > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            text += "…";
        }
        canvas.setColor(foreground);
        int x = leftAligned ? 14 : (button.getWidth() - metrics.stringWidth(text)) / 2;
        int y = (button.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        canvas.drawString(text, x, y);
        canvas.dispose();
    }
}

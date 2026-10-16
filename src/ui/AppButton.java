package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/** Quiet button hierarchy with hover and keyboard-focus states. */
final class AppButton extends JButton {
    enum Kind { PRIMARY, SECONDARY, QUIET, DANGER }
    private final Kind kind;

    AppButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(AppTheme.BODY.deriveFont(java.awt.Font.BOLD));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(AppTheme.spacing(9, 15, 9, 15));
        setPreferredSize(new Dimension(Math.max(96, getFontMetrics(getFont()).stringWidth(text) + 30), 39));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean hovered = getModel().isRollover() && isEnabled();
        Color fill = !isEnabled() ? AppTheme.RAISED : switch (kind) {
            case PRIMARY -> hovered ? AppTheme.HOVER : AppTheme.ACCENT;
            case SECONDARY -> hovered ? AppTheme.HOVER : AppTheme.RAISED;
            case QUIET -> hovered ? AppTheme.HOVER : AppTheme.BACKGROUND;
            case DANGER -> hovered ? AppTheme.HOVER : AppTheme.RAISED;
        };
        g.setColor(fill);
        g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 9, 9);
        if (kind == Kind.SECONDARY || kind == Kind.DANGER) {
            g.setColor(AppTheme.BORDER);
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 9, 9);
        }
        if (hasFocus()) {
            g.setColor(AppTheme.ACCENT);
            g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
        }
        g.dispose();
        setForeground(!isEnabled() ? AppTheme.MUTED : kind == Kind.PRIMARY && !hovered ? AppTheme.BACKGROUND
                : kind == Kind.DANGER ? AppTheme.DANGER : AppTheme.INK);
        super.paintComponent(graphics);
    }
}

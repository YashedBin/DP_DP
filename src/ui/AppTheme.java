package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** A small, vanilla-Swing visual vocabulary shared by every screen. */
final class AppTheme {
    static final Color BACKGROUND = new Color(0x111111);
    static final Color SURFACE = new Color(0x181818);
    static final Color RAISED = new Color(0x202020);
    static final Color BORDER = new Color(0x303030);
    static final Color INK = new Color(0xF1F1EF);
    static final Color SECONDARY = new Color(0xA3A3A0);
    static final Color MUTED = new Color(0x70706D);
    static final Color ACCENT = new Color(0xD5D5D0);
    static final Color HOVER = new Color(0x292929);
    static final Color SELECTION = new Color(0x333330);
    static final Color SUCCESS = new Color(0x87927D);
    static final Color WARNING = new Color(0xA69776);
    static final Color DANGER = new Color(0xA36F6B);

    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font SMALL = BODY.deriveFont(12f);
    static final Font MEDIUM = BODY.deriveFont(Font.BOLD, 15f);
    static final Font TITLE = new Font(Font.SERIF, Font.PLAIN, 27);
    static final Font BRAND = new Font(Font.SERIF, Font.BOLD, 25);
    static final Font QUESTION = new Font(Font.SERIF, Font.PLAIN, 20);

    private AppTheme() {}

    static void install() {
        UIManager.put("control", SURFACE);
        UIManager.put("controlText", INK);
        UIManager.put("text", RAISED);
        UIManager.put("textText", INK);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("OptionPane.questionIcon", null);
        UIManager.put("OptionPane.informationIcon", null);
        UIManager.put("TextField.background", RAISED);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("PasswordField.background", RAISED);
        UIManager.put("PasswordField.foreground", INK);
        UIManager.put("TextArea.background", RAISED);
        UIManager.put("TextArea.foreground", INK);
        UIManager.put("ComboBox.background", RAISED);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("List.background", SURFACE);
        UIManager.put("List.foreground", INK);
        UIManager.put("List.selectionBackground", SELECTION);
        UIManager.put("List.selectionForeground", INK);
        UIManager.put("RadioButton.background", SURFACE);
        UIManager.put("RadioButton.foreground", INK);
        UIManager.put("Button.background", RAISED);
        UIManager.put("Button.foreground", INK);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("SplitPane.background", BACKGROUND);
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.track", SURFACE);
        UIManager.put("ScrollBar.thumb", SELECTION);
        UIManager.put("Button.font", BODY);
        UIManager.put("Label.font", BODY);
        UIManager.put("TextField.font", BODY);
        UIManager.put("TextArea.font", BODY);
        UIManager.put("PasswordField.font", BODY);
        UIManager.put("List.font", BODY);
    }

    static JPanel page(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BACKGROUND);
        panel.setBorder(spacing(24, 32, 24, 32));
        return panel;
    }

    static JPanel surface(java.awt.LayoutManager layout, int padding) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), spacing(padding, padding, padding, padding)));
        return panel;
    }

    static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static Border spacing(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    static JLabel brand() { return label("EOMS", BRAND, INK); }
    static JLabel title(String text) { return label(text, TITLE, INK); }
    static JLabel muted(String text) { return label(text, SMALL, SECONDARY); }
    static JLabel section(String text) { return label(text, MEDIUM, INK); }

    static <T extends JTextField> T field(T field) {
        field.setFont(BODY);
        field.setForeground(INK);
        field.setBackground(RAISED);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(SELECTION);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                spacing(8, 11, 8, 11)));
        field.setPreferredSize(new Dimension(180, 40));
        return field;
    }

    static JTextArea area(JTextArea area) {
        area.setFont(BODY);
        area.setForeground(INK);
        area.setBackground(RAISED);
        area.setCaretColor(ACCENT);
        area.setSelectionColor(SELECTION);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(spacing(10, 12, 10, 12));
        return area;
    }

    static <T> JComboBox<T> combo(JComboBox<T> combo) {
        combo.setFont(BODY);
        combo.setForeground(INK);
        combo.setBackground(RAISED);
        combo.setRenderer((list, value, index, selected, focus) -> {
            JLabel row = label(value == null ? "" : value.toString(), BODY, INK);
            row.setOpaque(true);
            row.setBackground(selected ? SELECTION : RAISED);
            row.setBorder(spacing(6, 9, 6, 9));
            return row;
        });
        combo.setPreferredSize(new Dimension(180, 40));
        return combo;
    }

    static void radio(JRadioButton button) {
        button.setFont(BODY);
        button.setForeground(INK);
        button.setBackground(SURFACE);
        button.setIcon(radioIcon(false));
        button.setSelectedIcon(radioIcon(true));
    }

    private static Icon radioIcon(boolean selected) {
        return new Icon() {
            @Override public int getIconWidth() { return 20; }
            @Override public int getIconHeight() { return 20; }
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(selected ? ACCENT : SECONDARY);
                g.drawOval(x + 2, y + 2, 15, 15);
                if (selected) g.fillOval(x + 6, y + 6, 8, 8);
                g.dispose();
            }
        };
    }

    static JScrollPane scroll(Component content) {
        JScrollPane pane = new JScrollPane(content);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        pane.getViewport().setBackground(SURFACE);
        pane.setBackground(SURFACE);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        return pane;
    }

    static void sizeWindow(javax.swing.JFrame frame, int width, int height, int minWidth, int minHeight) {
        frame.setSize(width, height);
        frame.setMinimumSize(new Dimension(minWidth, minHeight));
        frame.setLocationRelativeTo(null);
    }

    static JLabel status(String status) {
        Color color = "ONGOING".equals(status) ? SUCCESS : "DRAFT".equals(status) ? WARNING : SECONDARY;
        JLabel badge = label(status, SMALL.deriveFont(Font.BOLD), color);
        badge.setOpaque(true);
        badge.setBackground(SELECTION);
        badge.setBorder(spacing(4, 9, 4, 9));
        return badge;
    }
}

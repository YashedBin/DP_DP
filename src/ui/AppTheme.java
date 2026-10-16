package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** A small, vanilla-Swing visual vocabulary shared by every screen. */
final class AppTheme {
    static final Color BACKGROUND = new Color(0xF5F2EC);
    static final Color SURFACE = new Color(0xFCFAF6);
    static final Color SOFT = new Color(0xEFEBE4);
    static final Color BORDER = new Color(0xDDD8CF);
    static final Color INK = new Color(0x292824);
    static final Color MUTED = new Color(0x74716A);
    static final Color ACCENT = new Color(0x6D7562);
    static final Color ACCENT_DARK = new Color(0x565E4D);
    static final Color DANGER = new Color(0x9B5550);
    static final Color SELECTION = new Color(0xE9ECE3);

    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font SMALL = BODY.deriveFont(12f);
    static final Font MEDIUM = BODY.deriveFont(Font.BOLD, 15f);
    static final Font TITLE = new Font(Font.SERIF, Font.PLAIN, 27);
    static final Font BRAND = new Font(Font.SERIF, Font.BOLD, 25);
    static final Font QUESTION = new Font(Font.SERIF, Font.PLAIN, 20);

    private AppTheme() {}

    static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", INK);
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

    static JLabel brand() { return label("YDVRC", BRAND, INK); }
    static JLabel title(String text) { return label(text, TITLE, INK); }
    static JLabel muted(String text) { return label(text, SMALL, MUTED); }
    static JLabel section(String text) { return label(text, MEDIUM, INK); }

    static <T extends JTextField> T field(T field) {
        field.setFont(BODY);
        field.setForeground(INK);
        field.setBackground(SURFACE);
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
        area.setBackground(SURFACE);
        area.setCaretColor(ACCENT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(spacing(10, 12, 10, 12));
        return area;
    }

    static <T> JComboBox<T> combo(JComboBox<T> combo) {
        combo.setFont(BODY);
        combo.setForeground(INK);
        combo.setBackground(SURFACE);
        combo.setPreferredSize(new Dimension(180, 40));
        return combo;
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
        JLabel badge = label(status, SMALL.deriveFont(Font.BOLD), ACCENT_DARK);
        badge.setOpaque(true);
        badge.setBackground(SELECTION);
        badge.setBorder(spacing(4, 9, 4, 9));
        return badge;
    }

    static void pad(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(spacing(top, left, bottom, right));
    }
}

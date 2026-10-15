package com.eoms;

import com.eoms.config.ApplicationContext;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        ApplicationContext context = new ApplicationContext();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing look and feel is still usable.
            }
            context.createWelcomeFrame().setVisible(true);
        });
    }
}

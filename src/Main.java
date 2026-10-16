import config.ApplicationContext;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        ApplicationContext context = new ApplicationContext();
        SwingUtilities.invokeLater(() -> {
            try {
                // Consistent component colors on Windows and Linux; native themes may ignore the dark palette.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing look and feel is still usable.
            }
            context.createLoginFrame().setVisible(true);
        });
    }
}

package patterns.bridge;

import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public final class SwingChannel implements NotificationChannel {
    private final Consumer<String> display;
    public SwingChannel(Consumer<String> display) { this.display = display; }
    @Override
    public void deliver(String recipient, String message) {
        SwingUtilities.invokeLater(() -> display.accept(message));
    }
}

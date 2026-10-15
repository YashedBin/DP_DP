package com.eoms.patterns.bridge;

import java.util.function.Consumer;

/** Console until a desktop window subscribes; then deliveries move to Swing. */
public final class DesktopChannel implements NotificationChannel {
    private final ConsoleChannel console = new ConsoleChannel();
    private volatile Consumer<String> display;

    public void showIn(Consumer<String> listener) { display = listener; }
    public void clear() { display = null; }

    @Override
    public void deliver(String recipient, String message) {
        Consumer<String> current = display;
        if (current == null) console.deliver(recipient, message);
        else new SwingChannel(current).deliver(recipient, message);
    }
}

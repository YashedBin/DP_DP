package patterns.bridge;

public final class ConsoleChannel implements NotificationChannel {
    @Override
    public void deliver(String recipient, String message) {
        System.out.println("[EOMS " + recipient + "] " + message);
    }
}

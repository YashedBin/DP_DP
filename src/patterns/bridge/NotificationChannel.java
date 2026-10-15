package patterns.bridge;

public interface NotificationChannel {
    void deliver(String recipient, String message);
}

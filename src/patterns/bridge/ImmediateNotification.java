package patterns.bridge;

public final class ImmediateNotification extends Notification {
    public ImmediateNotification(NotificationChannel channel) { super(channel); }
    @Override
    public void send(String recipient, String message) { channel.deliver(recipient, message); }
}

package com.eoms.patterns.bridge;

public abstract class Notification {
    protected final NotificationChannel channel;
    protected Notification(NotificationChannel channel) { this.channel = channel; }
    public abstract void send(String recipient, String message);
}

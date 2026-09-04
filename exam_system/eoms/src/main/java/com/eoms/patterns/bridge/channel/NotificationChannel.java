package com.eoms.patterns.bridge.channel;

public interface NotificationChannel {
    void send(String recipient, String message);
}

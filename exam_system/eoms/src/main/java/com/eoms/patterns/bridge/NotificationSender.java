package com.eoms.patterns.bridge;

import com.eoms.patterns.bridge.channel.NotificationChannel;

public abstract class NotificationSender {

    protected final NotificationChannel channel;

    protected NotificationSender(NotificationChannel channel) {
        this.channel = channel;
    }

    public abstract void notify(String recipient, String message);
}

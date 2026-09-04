package com.eoms.patterns.bridge.channel;

import org.springframework.stereotype.Component;

@Component
public class EmailChannel implements NotificationChannel {

    @Override
    public void send(String recipient, String message) {
        System.out.println("[EMAIL to " + recipient + "]: " + message);
    }
}

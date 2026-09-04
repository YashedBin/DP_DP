package com.eoms.patterns.bridge;

import com.eoms.patterns.bridge.channel.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class ImmediateSender extends NotificationSender {

    public ImmediateSender(NotificationChannel channel) {
        super(channel);
    }

    @Override
    public void notify(String recipient, String message) {
        channel.send(recipient, message);
    }
}

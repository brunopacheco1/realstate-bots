package com.github.brunopacheco1.realstatebots.consumers;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Notification;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.webhook.Payload;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class NotificationSender {

    @Incoming(PubSubConstants.SENDING_NOTIFICATION)
    public CompletionStage<Void> percolate(Message<Notification> message) {
        Notification notification = message.getPayload();
        Payload payload = Payload.builder().text(getText(notification)).build();
        try {
            Slack.getInstance().send(notification.getConfiguration().getWebhook(), payload);
            log.info("Sent notification.");
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        return message.ack();
    }

    private String getText(Notification notification) {
        String users = notification.getConfiguration().getUsers().stream().map(u -> "<" + u + ">")
                .collect(Collectors.joining(" "));
        return users + " " + notification.getUrl();
    }
}

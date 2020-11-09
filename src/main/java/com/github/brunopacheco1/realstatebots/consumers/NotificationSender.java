package com.github.brunopacheco1.realstatebots.consumers;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Notification;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.webhook.Payload;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class NotificationSender {

    @Incoming(PubSubConstants.SENDING_NOTIFICATION)
    @Merge
    public CompletionStage<Void> percolate(Message<Notification> message) {
        Notification notification = message.getPayload();
        Payload payload = Payload.builder().text(notification.getUrl()).build();
        try {
            for (String recipient : notification.getRecipients()) {
                Slack.getInstance().send(recipient, payload);
            }
            log.info("Sent notification.");
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        return message.ack();
    }
}

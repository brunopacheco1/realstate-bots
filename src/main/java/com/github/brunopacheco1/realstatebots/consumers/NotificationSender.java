package com.github.brunopacheco1.realstatebots.consumers;

import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Notification;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class NotificationSender {

    @Incoming(PubSubConstants.SENDING_NOTIFICATION)
    public CompletionStage<Void> percolate(Message<Notification> message) {
        Notification notification = message.getPayload();
        log.info(notification.toString());
        return message.ack();
    }
}

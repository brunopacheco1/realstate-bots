package com.github.brunopacheco1.realstatebots;

import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class NotificationSender {
    
    @Incoming("notification")
    public CompletionStage<Void> percolate(Message<Property> message) {
        Property property = message.getPayload();
        log.info(property.toString());
        return message.ack();
    }
}

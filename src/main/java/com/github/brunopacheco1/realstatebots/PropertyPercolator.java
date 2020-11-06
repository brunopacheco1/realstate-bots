package com.github.brunopacheco1.realstatebots;

import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class PropertyPercolator {
    
    @Incoming("incoming-property")
    @Outgoing("notification")
    @Broadcast
    public Property percolate(Message<Property> message) {
        Property property = message.getPayload();
        log.info(property.toString());
        return property;
    }
}

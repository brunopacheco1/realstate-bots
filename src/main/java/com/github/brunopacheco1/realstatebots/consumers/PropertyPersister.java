package com.github.brunopacheco1.realstatebots.consumers;

import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class PropertyPersister {

    @Incoming(PubSubConstants.INCOMING_PROPERTY)
    @Outgoing(PubSubConstants.PERCOLATING_PROPERTY)
    public Property persist(Message<Property> message) {
        Property property = message.getPayload();
        property.persist();
        // TODO - avoid duplicated entries
        return property;
    }
}

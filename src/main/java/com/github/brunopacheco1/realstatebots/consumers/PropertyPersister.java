package com.github.brunopacheco1.realstatebots.consumers;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.github.brunopacheco1.realstatebots.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;

@ApplicationScoped
public class PropertyPersister {

    @Inject
    @Channel(PubSubConstants.PERCOLATING_PROPERTY)
    Emitter<Property> propertyEmitter;

    @Incoming(PubSubConstants.INCOMING_PROPERTY)
    @Merge
    public CompletionStage<Void> persist(Message<Property> message) {
        Property property = message.getPayload();
        Optional<Property> exists = Property.findByIdOptional(property.getId());
        if (exists.isEmpty()) {
            property.persist();
            propertyEmitter.send(property);
        }
        return message.ack();
    }
}

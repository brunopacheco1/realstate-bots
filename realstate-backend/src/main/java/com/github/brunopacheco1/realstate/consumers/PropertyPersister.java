package com.github.brunopacheco1.realstate.consumers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.apache.commons.codec.digest.DigestUtils;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;

@ApplicationScoped
public class PropertyPersister {

    @Inject
    @Channel(PubSubConstants.RUNNING_PERCOLATOR)
    Emitter<Property> runningPercolatorEmitter;

    @Incoming(PubSubConstants.INCOMING_PROPERTY)
    @Merge
    public CompletionStage<Void> persist(Message<PropertyDto> message) {
        PropertyDto propertyDto = message.getPayload();

        LocalDateTime insertionDate = LocalDateTime.now();
        String id = DigestUtils.sha3_256Hex(propertyDto.getUrl() + propertyDto.getSource());

        Property property = new Property(id, propertyDto.getLocation(), propertyDto.getValue(),
                propertyDto.getPropertyType(), propertyDto.getTransactionType(), propertyDto.getUrl(),
                propertyDto.getSource(), insertionDate);

        Optional<Property> exists = Property.findByIdOptional(property.getId());
        if (exists.isEmpty()) {
            property.persist();
            runningPercolatorEmitter.send(property);
        }
        return message.ack();
    }
}

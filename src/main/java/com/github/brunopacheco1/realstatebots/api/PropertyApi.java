package com.github.brunopacheco1.realstatebots.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.github.brunopacheco1.realstatebots.consumers.PubSubConstants;
import com.github.brunopacheco1.realstatebots.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/api/v1/property")
public class PropertyApi {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<Property> incomingPropertyEmitter;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addProperty(Property property) {
        incomingPropertyEmitter.send(property);
    }
}

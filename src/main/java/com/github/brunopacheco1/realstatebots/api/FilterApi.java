package com.github.brunopacheco1.realstatebots.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.github.brunopacheco1.realstatebots.consumers.PubSubConstants;
import com.github.brunopacheco1.realstatebots.domain.Filter;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/api/v1/filter")
public class FilterApi {

    @Inject
    @Channel(PubSubConstants.INCOMING_FILTER)
    Emitter<Filter> filterEmitter;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addFilter(Filter filter) {
        filterEmitter.send(filter);
    }
}

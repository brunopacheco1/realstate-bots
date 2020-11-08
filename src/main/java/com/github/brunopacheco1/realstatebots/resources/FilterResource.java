package com.github.brunopacheco1.realstatebots.resources;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.github.brunopacheco1.realstatebots.domain.Filter;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/api/v1/filter")
public class FilterResource {

    @Inject
    @Channel("incoming-filter")
    private Emitter<Filter> filterEmitter;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public CompletionStage<Void> addFilter(Filter filter) {
        return filterEmitter.send(filter);
    }
}

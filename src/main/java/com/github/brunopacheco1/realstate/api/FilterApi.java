package com.github.brunopacheco1.realstate.api;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.domain.Filter;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/api/v1/filter")
public class FilterApi {

    @Inject
    @Channel(PubSubConstants.INCOMING_FILTER)
    Emitter<FilterDto> incomingFilterEmitter;

    @Inject
    @Channel(PubSubConstants.FINDING_PROPERTY)
    Emitter<Filter> findingPropertiesEmitter;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addFilter(FilterDto filterDto) {
        incomingFilterEmitter.send(filterDto);
    }

    @POST
    @Path("/{id}/run")
    @Consumes(MediaType.APPLICATION_JSON)
    public void runFilter(@PathParam("id") String id) {
        Optional<Filter> optional = Filter.findByIdOptional(new ObjectId(id));
        optional.ifPresent(findingPropertiesEmitter::send);
    }
}

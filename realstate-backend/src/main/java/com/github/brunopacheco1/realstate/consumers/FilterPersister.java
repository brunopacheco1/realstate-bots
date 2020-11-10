package com.github.brunopacheco1.realstate.consumers;

import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstate.domain.Filter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.annotations.Merge;

@ApplicationScoped
public class FilterPersister {

    @Incoming(PubSubConstants.INCOMING_FILTER)
    @Merge
    @Outgoing(PubSubConstants.UPDATING_PERCOLATOR)
    @Broadcast
    public Filter persist(Message<Filter> message) {
        Filter filter = message.getPayload();
        filter.persist();
        return filter;
    }
}

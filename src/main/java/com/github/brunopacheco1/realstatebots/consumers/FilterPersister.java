package com.github.brunopacheco1.realstatebots.consumers;

import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Filter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class FilterPersister {

    @Incoming(PubSubConstants.INCOMING_FILTER)
    @Outgoing(PubSubConstants.UPDATING_PERCOLATOR)
    @Broadcast
    public Filter persist(Message<Filter> message) {
        Filter filter = message.getPayload();
        log.info(filter.toString());
        return filter;
    }
}

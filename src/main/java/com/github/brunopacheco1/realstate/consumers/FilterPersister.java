package com.github.brunopacheco1.realstate.consumers;

import javax.enterprise.context.ApplicationScoped;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.FilterDto;
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
    public Filter persist(Message<FilterDto> message) {
        FilterDto filterDto = message.getPayload();
        Filter filter = new Filter(filterDto.getBudget(), filterDto.getPropertyType(), filterDto.getTransactionType(),
                filterDto.getRecipients());
        filter.persist();
        return filter;
    }
}

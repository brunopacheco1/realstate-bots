package com.github.brunopacheco1.realstatebots.consumers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.github.brunopacheco1.realstatebots.domain.Filter;
import com.github.brunopacheco1.realstatebots.domain.Notification;
import com.github.brunopacheco1.realstatebots.domain.Property;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.smallrye.reactive.messaging.annotations.Merge;

@ApplicationScoped
public class PropertiesFinder {

    @Inject
    @Channel(PubSubConstants.SENDING_NOTIFICATION)
    Emitter<Notification> notificationEmitter;

    @Incoming(PubSubConstants.FINDING_PROPERTY)
    @Merge
    public CompletionStage<Void> findProperties(Message<Filter> message) {
        Filter filter = message.getPayload();
        Pair<String, Map<String, Object>> query = getQuery(filter);
        PanacheQuery<Property> properties = Property.find(query.getLeft(), query.getRight());
        properties.page(Page.ofSize(25));
        do {
            Set<String> urls = properties.list().stream().map(Property::getUrl).collect(Collectors.toSet());
            for (String url : urls) {
                notificationEmitter.send(new Notification(filter.getRecipients(), url));
            }
        } while (properties.hasNextPage());
        return message.ack();
    }

    private Pair<String, Map<String, Object>> getQuery(Filter filter) {
        Map<String, Object> params = new HashMap<>();
        params.put("transactionType", filter.getTransactionType());
        String query = "transactionType = :transactionType";
        if (filter.getBudget() != null) {
            params.put("budget", filter.getBudget());
            query += " and value <= :budget";
        }
        if (filter.getPropertyType() != null) {
            params.put("propertyType", filter.getPropertyType());
            query += " and propertyType = :propertyType";
        }
        return Pair.of(query, params);
    }
}

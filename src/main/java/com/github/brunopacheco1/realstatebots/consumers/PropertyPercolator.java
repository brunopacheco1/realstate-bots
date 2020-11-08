package com.github.brunopacheco1.realstatebots.consumers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstatebots.consumers.TrieTreeQueryNode.Operation;
import com.github.brunopacheco1.realstatebots.domain.Filter;
import com.github.brunopacheco1.realstatebots.domain.Notification;
import com.github.brunopacheco1.realstatebots.domain.Property;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.runtime.Startup;
import lombok.extern.java.Log;

@Startup
@ApplicationScoped
@Log
public class PropertyPercolator {

    @Inject
    @Channel(PubSubConstants.SENDING_NOTIFICATION)
    Emitter<Notification> notificationEmitter;

    private final TrieTreeNode root = new TrieTreeNode(null);

    @PostConstruct
    void loadFilters() {
        List<Filter> filters = Filter.listAll();
        for (Filter filter : filters) {
            addFilter(filter);
        }
        log.info("Filters loaded.");
    }

    @Incoming(PubSubConstants.UPDATING_PERCOLATOR)
    public CompletionStage<Void> addFilter(Message<Filter> message) {
        Filter filter = message.getPayload();
        addFilter(filter);
        return message.ack();
    }

    private void addFilter(Filter filter) {
        TrieTreeNode budgetNode = new TrieTreeNode(filter.getBudget(), filter.asRecipientsConfigurations());
        TrieTreeNode propertyTypeNode = new TrieTreeNode(filter.getPropertyType());
        TrieTreeNode transactionTypeNode = new TrieTreeNode(filter.getTransactionType());

        propertyTypeNode.insert(budgetNode);
        transactionTypeNode.insert(propertyTypeNode);
        root.insert(transactionTypeNode);
    }

    @Incoming(PubSubConstants.PERCOLATING_PROPERTY)
    public CompletionStage<Void> percolate(Message<Property> message) {
        Property property = message.getPayload();
        TrieTreeQueryNode query = getQuery(property);
        Set<SlackRecipientsConfiguration> configs = root.query(query);

        if (!configs.isEmpty()) {
            String body = property.getUrl();
            for (SlackRecipientsConfiguration config : configs) {
                Notification notification = new Notification(config, body);
                notificationEmitter.send(notification);
            }
        }
        return message.ack();
    }

    private TrieTreeQueryNode getQuery(Property property) {
        return TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getTransactionType())
                .next(TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getPropertyType()).next(
                        TrieTreeQueryNode.builder().operation(Operation.LESSER).value(property.getValue()).build())
                        .build())
                .build();
    }
}

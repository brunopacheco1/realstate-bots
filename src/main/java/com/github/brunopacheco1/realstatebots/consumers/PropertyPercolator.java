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
import io.smallrye.reactive.messaging.annotations.Merge;
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
    @Merge
    public CompletionStage<Void> addFilter(Message<Filter> message) {
        Filter filter = message.getPayload();
        addFilter(filter);
        return message.ack();
    }

    private void addFilter(Filter filter) {
        TrieTreeNode budgetNode = new TrieTreeNode(filter.getBudget(), filter.getRecipients());
        TrieTreeNode propertyTypeNode = new TrieTreeNode(filter.getPropertyType());
        TrieTreeNode transactionTypeNode = new TrieTreeNode(filter.getTransactionType());

        propertyTypeNode.insert(budgetNode);
        transactionTypeNode.insert(propertyTypeNode);
        root.insert(transactionTypeNode);
    }

    @Incoming(PubSubConstants.RUNNING_PERCOLATOR)
    @Merge
    public CompletionStage<Void> percolate(Message<Property> message) {
        Property property = message.getPayload();
        TrieTreeQueryNode query = getQuery(property);
        Set<String> recipients = root.query(query);

        if (!recipients.isEmpty()) {
            Notification notification = new Notification(recipients, property.getUrl());
            notificationEmitter.send(notification);
        }
        return message.ack();
    }

    private TrieTreeQueryNode getQuery(Property property) {
        return TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getTransactionType())
                .next(TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getPropertyType()).next(
                        TrieTreeQueryNode.builder().operation(Operation.GREATER).value(property.getValue()).build())
                        .build())
                .build();
    }
}

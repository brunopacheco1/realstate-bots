package com.github.brunopacheco1.realstatebots.consumers;

import java.util.Set;
import java.util.concurrent.CompletionStage;

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

@ApplicationScoped
public class PropertyPercolator {

    @Inject
    @Channel(PubSubConstants.SENDING_NOTIFICATION)
    Emitter<Notification> notificationEmitter;

    private TrieTreeNode root = new TrieTreeNode(null);

    // TODO - Load all filters on boot time

    @Incoming(PubSubConstants.UPDATING_PERCOLATOR)
    public CompletionStage<Void> addFilter(Message<Filter> message) {
        Filter filter = message.getPayload();

        TrieTreeNode budgetNode = new TrieTreeNode(filter.getBudget(), filter.getRecipients());
        TrieTreeNode propertyTypeNode = new TrieTreeNode(filter.getPropertyType());
        TrieTreeNode transactionTypeNode = new TrieTreeNode(filter.getTransactionType());

        propertyTypeNode.insert(budgetNode);
        transactionTypeNode.insert(propertyTypeNode);
        root.insert(transactionTypeNode);

        return message.ack();
    }

    @Incoming(PubSubConstants.PERCOLATING_PROPERTY)
    public CompletionStage<Void> percolate(Message<Property> message) {
        Property property = message.getPayload();
        TrieTreeQueryNode query = getQuery(property);
        Set<String> recipients = root.query(query);

        if (!recipients.isEmpty()) {
            String body = property.getUrl();
            Notification notification = new Notification(recipients, body);
            notificationEmitter.send(notification);
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

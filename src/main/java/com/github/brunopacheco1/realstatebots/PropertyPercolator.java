package com.github.brunopacheco1.realstatebots;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.TrieTreeQueryNode.Operation;
import com.github.brunopacheco1.realstatebots.domain.Filter;
import com.github.brunopacheco1.realstatebots.domain.Notification;
import com.github.brunopacheco1.realstatebots.domain.Property;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class PropertyPercolator {

    private TrieTreeNode root = new TrieTreeNode(null);

    @Incoming("incoming-filter")
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

    @Incoming("incoming-property")
    @Outgoing("notification")
    @Broadcast
    public Notification percolate(Message<Property> message) {
        Property property = message.getPayload();
        TrieTreeQueryNode query = getQuery(property);
        Set<String> recipients = root.query(query);
        log.info(String.join(", ", recipients));
        String body = property.getUrl();
        return new Notification(recipients, body);
    }

    private TrieTreeQueryNode getQuery(Property property) {
        return TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getTransactionType())
                .next(TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getPropertyType()).next(
                        TrieTreeQueryNode.builder().operation(Operation.LESSER).value(property.getValue()).build())
                        .build())
                .build();
    }
}

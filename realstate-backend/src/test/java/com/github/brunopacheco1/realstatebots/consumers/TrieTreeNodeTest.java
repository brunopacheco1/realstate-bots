package com.github.brunopacheco1.realstate.consumers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;
import com.github.brunopacheco1.realstate.consumers.TrieTreeQueryNode.Operation;
import com.github.brunopacheco1.realstate.domain.Filter;
import com.github.brunopacheco1.realstate.domain.Property;
import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.TransactionType;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TrieTreeNodeTest {

        private TrieTreeNode root;

        @BeforeEach
        public void setUp() {
                root = new TrieTreeNode(null);

                addFilterToTree(new Filter(BigDecimal.valueOf(600000), null, TransactionType.BUY,
                                Sets.newHashSet("email")));
                addFilterToTree(new Filter(BigDecimal.valueOf(700000), PropertyType.HOUSE, TransactionType.BUY,
                                Sets.newHashSet("email1")));
                addFilterToTree(new Filter(BigDecimal.valueOf(900000), null, TransactionType.BUY,
                                Sets.newHashSet("email2")));
        }

        private void addFilterToTree(Filter filter) {
                TrieTreeNode budgetNode = new TrieTreeNode(filter.getBudget(), filter.getRecipients());
                TrieTreeNode propertyTypeNode = new TrieTreeNode(filter.getPropertyType());
                TrieTreeNode transactionTypeNode = new TrieTreeNode(filter.getTransactionType());

                propertyTypeNode.insert(budgetNode);
                transactionTypeNode.insert(propertyTypeNode);
                root.insert(transactionTypeNode);
        }

        @Test
        public void should_return_none_recipients_config_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(1000000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.isEmpty());
        }

        @Test
        public void should_return_one_recipients_config_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(800000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 1);
        }

        @Test
        public void should_return_two_recipients_configs_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(700000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 2);
        }

        @Test
        public void should_return_three_recipients_configs_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(600000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 3);
        }

        @Test
        public void should_return_three_recipients_configs_given_property_type() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(600000),
                                PropertyType.HOUSE, TransactionType.BUY, null, null, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 3);
        }

        private TrieTreeQueryNode getQuery(Property property) {
                return TrieTreeQueryNode.builder().operation(Operation.EQUALS).value(property.getTransactionType())
                                .next(TrieTreeQueryNode.builder().operation(Operation.EQUALS)
                                                .value(property.getPropertyType())
                                                .next(TrieTreeQueryNode.builder().operation(Operation.GREATER)
                                                                .value(property.getValue()).build())
                                                .build())
                                .build();
        }
}

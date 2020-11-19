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
                                Sets.newHashSet("email"), 3, false));
                addFilterToTree(new Filter(BigDecimal.valueOf(700000), PropertyType.HOUSE, TransactionType.BUY,
                                Sets.newHashSet("email1"), 3, false));
                addFilterToTree(new Filter(BigDecimal.valueOf(900000), null, TransactionType.BUY,
                                Sets.newHashSet("email2"), 3, false));
                addFilterToTree(new Filter(BigDecimal.valueOf(600000), PropertyType.HOUSE, TransactionType.BUY,
                                Sets.newHashSet("email3"), 2, true));
                addFilterToTree(new Filter(BigDecimal.valueOf(2000000), PropertyType.HOUSE, TransactionType.BUY,
                                Sets.newHashSet("email4"), 3, true));
                addFilterToTree(new Filter(BigDecimal.valueOf(2000000), PropertyType.HOUSE, TransactionType.BUY,
                                Sets.newHashSet("email5"), null, true));
        }

        private void addFilterToTree(Filter filter) {
                TrieTreeNode hasGarageNode = new TrieTreeNode(filter.getHasGarage(), filter.getRecipients());
                TrieTreeNode numberOfBedroomsNode = new TrieTreeNode(filter.getNumberOfBedrooms());
                TrieTreeNode budgetNode = new TrieTreeNode(filter.getBudget());
                TrieTreeNode propertyTypeNode = new TrieTreeNode(filter.getPropertyType());
                TrieTreeNode transactionTypeNode = new TrieTreeNode(filter.getTransactionType());

                numberOfBedroomsNode.insert(hasGarageNode);
                budgetNode.insert(numberOfBedroomsNode);
                propertyTypeNode.insert(budgetNode);
                transactionTypeNode.insert(propertyTypeNode);
                root.insert(transactionTypeNode);
        }

        @Test
        public void should_return_none_recipient_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(1000000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, 3, false, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.isEmpty());
        }

        @Test
        public void should_return_one_recipient_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(800000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, 3, false, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 1);
        }

        @Test
        public void should_return_two_recipients_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(700000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, 3, false, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 2);
        }

        @Test
        public void should_return_three_recipients_given_budget() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(600000),
                                PropertyType.APPARTMENT, TransactionType.BUY, null, null, 3, false, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 3);
        }

        @Test
        public void should_return_three_recipients_given_property_type() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(600000),
                                PropertyType.HOUSE, TransactionType.BUY, null, null, 3, false, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 3);
        }

        @Test
        public void should_return_two_recipients_given_number_of_bedrooms() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(600000),
                                PropertyType.HOUSE, TransactionType.BUY, null, null, 2, true, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 2);
        }

        @Test
        public void should_return_one_recipient_even_when_numberOfbedrooms_is_null() {
                TrieTreeQueryNode query = getQuery(new Property(null, null, BigDecimal.valueOf(2000000),
                                PropertyType.HOUSE, TransactionType.BUY, null, null, null, true, null));
                Set<String> recipients = root.query(query);
                assertTrue(recipients.size() == 1);
                assertTrue(recipients.contains("email5"));
        }

        private TrieTreeQueryNode getQuery(Property property) {
                TrieTreeQueryNode hasGarageNode = new TrieTreeQueryNode(property.getHasGarage(), Operation.EQUALS);
                TrieTreeQueryNode numberOfBedroomsNode = new TrieTreeQueryNode(property.getNumberOfBedrooms(),
                                Operation.LESS);
                TrieTreeQueryNode valueNode = new TrieTreeQueryNode(property.getValue(), Operation.GREATER);
                TrieTreeQueryNode propertyTypeNode = new TrieTreeQueryNode(property.getPropertyType(),
                                Operation.EQUALS);
                TrieTreeQueryNode transactionTypeNode = new TrieTreeQueryNode(property.getTransactionType(),
                                Operation.EQUALS);

                transactionTypeNode.next(propertyTypeNode).next(valueNode).next(numberOfBedroomsNode)
                                .next(hasGarageNode);

                return transactionTypeNode;
        }
}

package com.github.brunopacheco1.realstatebots;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import com.github.brunopacheco1.realstatebots.TrieTreeQueryNode.Operation;

public class TrieTreeNode {

    private final Object key;
    private final TreeMap<Object, TrieTreeNode> children = new TreeMap<>();
    private TrieTreeNode defaultPath;
    private final Set<String> recipients;

    public TrieTreeNode(Object key) {
        this.key = key;
        this.recipients = new HashSet<>();
    }

    public TrieTreeNode(Object key, Set<String> recipients) {
        this.key = key;
        this.recipients = recipients;
    }

    public void insert(TrieTreeNode newNode) {
        if (newNode.key == null && defaultPath != null) {
            defaultPath.recipients.addAll(newNode.recipients);
            for (TrieTreeNode newNodeChild : newNode.children.values()) {
                defaultPath.insert(newNodeChild);
                for (TrieTreeNode child : children.values()) {
                    child.insert(newNodeChild);
                }
            }
        } else if (newNode.key == null && defaultPath == null) {
            defaultPath = newNode;
            for (TrieTreeNode newNodeChild : newNode.children.values()) {
                for (TrieTreeNode child : children.values()) {
                    child.insert(newNodeChild);
                }
            }
        } else if (children.containsKey(newNode.key)) {
            TrieTreeNode existingChild = children.get(newNode.key);
            existingChild.recipients.addAll(newNode.recipients);
            for (TrieTreeNode newNodeChild : newNode.children.values()) {
                existingChild.insert(newNodeChild);
                if (defaultPath != null) {
                    defaultPath.insert(newNodeChild);
                }
            }
        } else {
            children.put(newNode.key, newNode);
            for (TrieTreeNode newNodeChild : newNode.children.values()) {
                if (defaultPath != null) {
                    defaultPath.insert(newNodeChild);
                }
            }
        }
    }

    public Set<String> query(TrieTreeQueryNode queryNode) {
        if (queryNode == null) {
            return recipients;
        }

        if (queryNode.getValue() == null) {
            if (defaultPath == null) {
                return Collections.emptySet();
            }

            return defaultPath.query(queryNode.getNext());
        }

        Collection<TrieTreeNode> childrenToCheck;
        if (queryNode.getOperation() == Operation.EQUALS) {
            TrieTreeNode child = children.get(queryNode.getValue());

            if (child == null) {
                return Collections.emptySet();
            }

            childrenToCheck = Collections.singleton(child);
        } else if (queryNode.getOperation() == Operation.LESSER) {
            try {
                childrenToCheck = children.headMap(key, true).values();
            } catch (IllegalArgumentException e) {
                childrenToCheck = Collections.emptyList();
            }
        } else {
            try {
                childrenToCheck = children.tailMap(key, true).values();
            } catch (IllegalArgumentException e) {
                childrenToCheck = Collections.emptyList();
            }
        }

        Set<String> flatMappedRecipients = new HashSet<>();
        for (TrieTreeNode childToCheck : childrenToCheck) {
            flatMappedRecipients.addAll(childToCheck.query(queryNode.getNext()));
        }

        return flatMappedRecipients;
    }
}

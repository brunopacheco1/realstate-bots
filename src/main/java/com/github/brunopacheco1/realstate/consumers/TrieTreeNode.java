package com.github.brunopacheco1.realstate.consumers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import com.github.brunopacheco1.realstate.consumers.TrieTreeQueryNode.Operation;
import lombok.extern.java.Log;

@Log
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
        List<TrieTreeNode> toReceiveChildren = new ArrayList<>();

        if (newNode.key == null) {
            if (defaultPath == null) {
                defaultPath = newNode;
            } else {
                toReceiveChildren.add(defaultPath);
            }
        } else if (children.containsKey(newNode.key)) {
            if (defaultPath != null) {
                toReceiveChildren.add(defaultPath);
            }
            toReceiveChildren.add(children.get(newNode.key));
        } else {
            children.put(newNode.key, newNode);
            if (defaultPath != null) {
                toReceiveChildren.add(defaultPath);
            }
        }

        for (TrieTreeNode nodeChild : toReceiveChildren) {
            nodeChild.recipients.addAll(newNode.recipients);
            if (newNode.defaultPath != null) {
                nodeChild.insert(newNode.defaultPath);
            }
            for (TrieTreeNode newNodeChild : newNode.children.values()) {
                nodeChild.insert(newNodeChild);
            }
        }
    }

    public Set<String> query(TrieTreeQueryNode queryNode) {
        if (queryNode == null) {
            return recipients;
        }

        List<TrieTreeNode> nodesToCheck = new ArrayList<>();
        
        if (queryNode.getValue() != null) {
            if (queryNode.getOperation() == Operation.EQUALS) {
                TrieTreeNode child = children.get(queryNode.getValue());
                if (child != null) {
                    nodesToCheck.add(child);
                }
            } else if (queryNode.getOperation() == Operation.LESS) {
                try {
                    nodesToCheck.addAll(children.headMap(queryNode.getValue(), true).values());
                } catch (IllegalArgumentException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                }
            } else {
                try {
                    nodesToCheck.addAll(children.tailMap(queryNode.getValue(), true).values());
                } catch (IllegalArgumentException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }

        if (defaultPath != null) {
            nodesToCheck.add(defaultPath);
        }

        Set<String> flatMappedRecipients = new HashSet<>();
        for (TrieTreeNode childToCheck : nodesToCheck) {
            flatMappedRecipients.addAll(childToCheck.query(queryNode.getNext()));
        }

        return flatMappedRecipients;
    }
}

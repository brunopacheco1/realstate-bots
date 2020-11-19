package com.github.brunopacheco1.realstate.consumers;

import lombok.Getter;

@Getter
public class TrieTreeQueryNode {

    private final Object value;
    private final Operation operation;
    private TrieTreeQueryNode next;

    public TrieTreeQueryNode(Object value, Operation operation) {
        this.value = value;
        this.operation = operation;
    }

    public TrieTreeQueryNode next(TrieTreeQueryNode node) {
        this.next = node;
        return node;
    }

    enum Operation {
        LESS, EQUALS, GREATER
    }
}

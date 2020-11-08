package com.github.brunopacheco1.realstatebots.consumers;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrieTreeQueryNode {
    
    private Object value;
    private Operation operation;
    private TrieTreeQueryNode next;

    enum Operation {
        LESSER,
        EQUALS,
        GREATER
    }
}

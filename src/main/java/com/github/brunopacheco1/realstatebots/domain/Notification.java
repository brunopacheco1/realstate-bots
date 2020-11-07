package com.github.brunopacheco1.realstatebots.domain;

import java.util.Set;

import lombok.Data;

@Data
public class Notification {
    private final Set<String> recipients;
    private final String body;
}

package com.github.brunopacheco1.realstatebots.domain;

import lombok.Data;

@Data
public class Notification {
    private final String recipient;
    private final String body;
}

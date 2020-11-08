package com.github.brunopacheco1.realstatebots.consumers;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlackRecipientsConfiguration {
    private String webhook;
    private Set<String> users;
}

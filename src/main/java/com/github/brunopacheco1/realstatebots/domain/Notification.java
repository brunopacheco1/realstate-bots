package com.github.brunopacheco1.realstatebots.domain;

import com.github.brunopacheco1.realstatebots.consumers.SlackRecipientsConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    private SlackRecipientsConfiguration configuration;
    private String url;
}

package com.github.brunopacheco1.realstatebots.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.github.brunopacheco1.realstatebots.consumers.SlackRecipientsConfiguration;

import org.bson.codecs.pojo.annotations.BsonIgnore;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "filters")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Filter extends PanacheMongoEntity {
    private BigDecimal budget;
    private PropertyType propertyType;
    private TransactionType transactionType;
    private String webhook;
    private Set<String> users;

    @BsonIgnore
    public Set<SlackRecipientsConfiguration> asRecipientsConfigurations() {
        Set<SlackRecipientsConfiguration> configs = new HashSet<>();
        configs.add(new SlackRecipientsConfiguration(webhook, users));
        return configs;
    }
}

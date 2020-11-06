package com.github.brunopacheco1.realstatebots.domain;

import java.math.BigDecimal;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;

@MongoEntity(collection="properties")
@Data
public class Property extends PanacheMongoEntity {

    private final String location;
    private final BigDecimal value;
    private final PropertyType propertyType;
    private final TransactionType transactionType;
    private final String url;
    private final Source source;
}

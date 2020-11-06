package com.github.brunopacheco1.realstatebots.domain;

import java.math.BigDecimal;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;

@MongoEntity(collection="filters")
@Data
public class Filter extends PanacheMongoEntity {
    private final BigDecimal budget;
    private final PropertyType propertyType;
    private final TransactionType transactionType;
}

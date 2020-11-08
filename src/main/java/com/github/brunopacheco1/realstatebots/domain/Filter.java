package com.github.brunopacheco1.realstatebots.domain;

import java.math.BigDecimal;
import java.util.Set;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MongoEntity(collection="filters")
@Data
@EqualsAndHashCode(callSuper = true)
public class Filter extends PanacheMongoEntity {
    private final BigDecimal budget;
    private final PropertyType propertyType;
    private final TransactionType transactionType;
    private final Set<String> recipients;
}

package com.github.brunopacheco1.realstatebots.domain;

import java.math.BigDecimal;
import java.util.Set;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection="filters")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Filter extends PanacheMongoEntity {
    private BigDecimal budget;
    private PropertyType propertyType;
    private TransactionType transactionType;
    private Set<String> recipients;
}

package com.github.brunopacheco1.realstate.domain;

import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.TransactionType;
import com.github.brunopacheco1.realstate.api.Source;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "properties")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Property extends PanacheMongoEntityBase {
    @BsonId
    private String id;
    private String location;
    private BigDecimal value;
    private PropertyType propertyType;
    private TransactionType transactionType;
    private String url;
    private Source source;
    private Integer numberOfBedrooms;
    private Boolean hasGarage;
    private LocalDateTime insertionDate;
}

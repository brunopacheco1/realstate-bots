package com.github.brunopacheco1.realstate.api;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyDto {
    private String location;
    private BigDecimal value;
    private PropertyType propertyType;
    private TransactionType transactionType;
    private String url;
    private Source source;
    private Integer numberOfBedrooms;
    private Boolean hasGarage;
}

package com.github.brunopacheco1.realstate.api;

import java.math.BigDecimal;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilterDto {
    private BigDecimal budget;
    private PropertyType propertyType;
    private TransactionType transactionType;
    private Set<String> recipients;
    private Integer numberOfBedrooms;
    private Boolean hasGarage;
}

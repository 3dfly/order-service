package com.threedfly.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintQuotationResponse {
    private String fileName;
    private Double materialUsedGrams;
    private Integer printingTimeMinutes;
    private String technology;
    private String material;
    private Double layerHeight;
    private Integer shells;
    private Integer infill;
    private Boolean supporters;
    private BigDecimal estimatedPrice;
    private String currency;

    // Price breakdown
    private BigDecimal pricePerGram;
    private BigDecimal pricePerMinute;
    private BigDecimal materialCost;
    private BigDecimal timeCost;
}

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
public class PrintingCalculationResponse {
    
    private BigDecimal totalPrice;
    private double weightGrams;
    private int printingTimeMinutes;
    private BigDecimal pricePerGram;
    private BigDecimal pricePerMinute;
    private BigDecimal weightCost;
    private BigDecimal timeCost;
    private String filename;
    private String status;
    private String message;
} 
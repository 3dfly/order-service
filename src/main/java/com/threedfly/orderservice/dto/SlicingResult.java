package com.threedfly.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlicingResult {
    
    private double filamentWeightGrams;
    private int estimatedPrintTimeMinutes;
    private String filamentType;
    private double filamentLengthMm;
    private int layerCount;
    private boolean success;
    private String errorMessage;
} 
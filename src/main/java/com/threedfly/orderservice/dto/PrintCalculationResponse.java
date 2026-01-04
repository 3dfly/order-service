package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.enums.BrimType;
import com.threedfly.orderservice.enums.InfillPattern;
import com.threedfly.orderservice.enums.SeamPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintCalculationResponse {
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

    // New optional parameters used in the quotation
    private BrimType brimType;
    private Integer brimWidth;
    private Integer topShellLayers;
    private Integer bottomShellLayers;
    private InfillPattern infillPattern;
    private SeamPosition seam;
    private Boolean autoOrient;
    private String colorChange;
}

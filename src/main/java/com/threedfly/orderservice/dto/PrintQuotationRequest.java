package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.validation.ValidMaterialCombination;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidMaterialCombination
public class PrintQuotationRequest {

    @NotBlank(message = "Technology is required")
    @Pattern(regexp = "FDM|SLS|SLA", message = "Technology must be FDM, SLS, or SLA")
    private String technology;

    @NotBlank(message = "Material is required")
    @Pattern(regexp = "PLA|ABS|PETG|TPU", message = "Material must be PLA, ABS, PETG, or TPU")
    private String material;

    @NotNull(message = "Layer height is required")
    @DecimalMin(value = "0.05", message = "Layer height must be at least 0.05mm")
    @DecimalMax(value = "0.4", message = "Layer height must not exceed 0.4mm")
    private Double layerHeight;

    @NotNull(message = "Shells count is required")
    @Min(value = 1, message = "Shells must be at least 1")
    @Max(value = 5, message = "Shells must not exceed 5")
    private Integer shells;

    @NotNull(message = "Infill percentage is required")
    @Min(value = 5, message = "Infill must be at least 5%")
    @Max(value = 20, message = "Infill must not exceed 20%")
    private Integer infill;

    @NotNull(message = "Supporters parameter is required")
    private Boolean supporters;
}

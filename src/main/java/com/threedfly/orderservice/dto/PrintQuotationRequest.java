package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.enums.BrimType;
import com.threedfly.orderservice.enums.InfillPattern;
import com.threedfly.orderservice.enums.SeamPosition;
import com.threedfly.orderservice.enums.SupportType;
import com.threedfly.orderservice.validation.ValidMaterialCombination;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
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

    // New optional parameters with defaults
    @Builder.Default
    private BrimType brimType = BrimType.AUTO;

    @Min(value = 0, message = "Brim width must be at least 0mm")
    @Max(value = 20, message = "Brim width must not exceed 20mm")
    private Integer brimWidth;

    /**
     * Type of support structure to generate.
     * If not specified and supporters=true, will default to NORMAL.
     * This parameter is ignored if supporters=false.
     */
    private SupportType supportType;

    @Builder.Default
    @Min(value = 0, message = "Top shell layers must be at least 0")
    @Max(value = 10, message = "Top shell layers must not exceed 10")
    private Integer topShellLayers = 5;

    @Builder.Default
    @Min(value = 0, message = "Bottom shell layers must be at least 0")
    @Max(value = 10, message = "Bottom shell layers must not exceed 10")
    private Integer bottomShellLayers = 3;

    @Builder.Default
    private InfillPattern infillPattern = InfillPattern.GRID;

    private SeamPosition seam;

    /**
     * Auto-orient the model for optimal printing.
     * When enabled, the system will analyze the model geometry and rotate it
     * to the optimal orientation (largest flat surface on build plate, minimized height).
     * This is done via pre-processing before slicing, using mesh analysis algorithms.
     * Requires Python 3 with trimesh library installed.
     */
    @Builder.Default
    private Boolean autoOrient = true;

    /**
     * Color change layers - only applicable for 3MF files.
     * Comma-separated layer numbers where filament color should change (e.g., "10,20,30").
     * The printer will pause at these layers to allow manual filament change.
     */
    @Pattern(regexp = "^\\d+(,\\d+)*$", message = "Color change must be comma-separated layer numbers (e.g., '10,20,30')")
    private String colorChange;
}

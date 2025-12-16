package com.threedfly.orderservice.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "printing.pricing")
@Data
@Validated
public class PrintingPricingConfig {

    @NotNull
    private Map<String, BigDecimal> technology;

    @NotNull
    private Map<String, MaterialConfig> material;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal shellCostFactor;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal standardLayerHeight;

    public BigDecimal getTechnologyCost(String technology) {
        return this.technology.getOrDefault(technology, BigDecimal.ONE);
    }

    public MaterialConfig getMaterialConfig(String material) {
        MaterialConfig config = this.material.get(material);
        if (config == null) {
            throw new IllegalArgumentException("Unknown material: " + material);
        }
        return config;
    }

    @Data
    public static class MaterialConfig {
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal density; // g/cm³

        @NotNull
        @DecimalMin("0.0")
        private BigDecimal pricePerGram; // USD per gram
    }
}

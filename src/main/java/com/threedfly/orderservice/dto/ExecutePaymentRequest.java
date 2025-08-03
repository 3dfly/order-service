package com.threedfly.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePaymentRequest {
    
    @NotBlank(message = "Provider payment ID is required")
    private String providerPaymentId;
    
    @NotBlank(message = "Provider payer ID is required")
    private String providerPayerId;
} 
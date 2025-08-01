package com.threedfly.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePaymentRequest {
    
    @NotBlank(message = "PayPal payment ID is required")
    private String paypalPaymentId;
    
    @NotBlank(message = "PayPal payer ID is required")
    private String paypalPayerId;
} 
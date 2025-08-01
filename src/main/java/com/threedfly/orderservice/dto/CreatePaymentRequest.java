package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal totalAmount;
    
    // PayPal specific fields
    @Email(message = "Valid PayPal email is required")
    private String paypalEmail;
    
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD)")
    private String currency = "USD";
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    // Success and cancel URLs for PayPal redirect
    @NotBlank(message = "Success URL is required")
    @Pattern(regexp = "^https?://.*", message = "Success URL must be a valid URL")
    private String successUrl;
    
    @NotBlank(message = "Cancel URL is required")
    @Pattern(regexp = "^https?://.*", message = "Cancel URL must be a valid URL")
    private String cancelUrl;
} 
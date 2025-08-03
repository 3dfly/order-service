package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

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
    
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD)")
    private String currency = "USD";
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    // Success and cancel URLs for redirect-based payment methods
    @NotBlank(message = "Success URL is required")
    @Pattern(regexp = "^https?://.*", message = "Success URL must be a valid URL")
    private String successUrl;
    
    @NotBlank(message = "Cancel URL is required")
    @Pattern(regexp = "^https?://.*", message = "Cancel URL must be a valid URL")
    private String cancelUrl;
    
    // Provider-specific data (flexible for different payment providers)
    private Map<String, Object> providerData;
    
    // Convenience method for PayPal email (backward compatibility)
    public String getPaypalEmail() {
        return providerData != null ? (String) providerData.get("email") : null;
    }
    
    public void setPaypalEmail(String email) {
        if (providerData == null) {
            providerData = new java.util.HashMap<>();
        }
        providerData.put("email", email);
    }
} 
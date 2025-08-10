package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

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

    public void setProviderData(Map<String, Object> providerData) {
        this.providerData = providerData;
    }

    // Add provider-specific data (e.g., PayPal email)
    public void addProviderData(String key, Object value) {
        if (this.providerData == null) {
            this.providerData = new HashMap<>();
        }
        this.providerData.put(key, value);
    }

    // Convenience methods for PayPal email
    public void setPaypalEmail(String email) {
        addProviderData("email", email);
    }

    public String getPaypalEmail() {
        if (providerData != null && providerData.containsKey("email")) {
            return (String) providerData.get("email");
        }
        return null;
    }
}
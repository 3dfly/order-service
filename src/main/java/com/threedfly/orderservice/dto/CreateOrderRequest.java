package com.threedfly.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "Seller ID is required")
    private Long sellerId;
    
    @NotNull(message = "Supplier ID is required")
    private Long supplierId;
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    
    @NotBlank(message = "STL file URL is required")
    @Pattern(regexp = "^https?://.*", message = "STL file URL must be a valid URL")
    private String stlFileUrl;
    
    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddress shippingAddress;
}
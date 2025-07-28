package com.threedfly.orderservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Supplier ID is required")
    private Long supplierId;
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "Seller ID is required")
    private Long sellerId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    
    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    private double totalPrice;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;
} 
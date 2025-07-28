package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    private Double totalPrice;
    
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;
    
    @Email(message = "Valid email is required")
    private String customerEmail;
    
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;
    
    private OrderStatus status;
} 
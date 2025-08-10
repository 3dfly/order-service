package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.OrderStatus;
import jakarta.validation.Valid;
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
    
    @Pattern(regexp = "^https?://.*", message = "STL file URL must be a valid URL")
    private String stlFileUrl;
    
    @Valid
    private ShippingAddress shippingAddress;
    
    private OrderStatus status;
}
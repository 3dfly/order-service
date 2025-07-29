package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long productId;
    private Long supplierId;
    private Long customerId;
    private Long sellerId;
    private int quantity;
    private double totalPrice;
    private LocalDateTime orderDate;
    private String customerName;
    private String customerEmail;
    private String shippingAddress;
    private OrderStatus status;
    private SellerResponse seller;
} 
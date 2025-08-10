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
    private String productId;
    private Long supplierId;
    private Long customerId;
    private Long sellerId;
    private int quantity;
    private String stlFileUrl;
    private ShippingAddress shippingAddress;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private SellerResponse seller;
}
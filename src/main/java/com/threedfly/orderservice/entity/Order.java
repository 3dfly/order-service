package com.threedfly.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String productId; // Reference to Product in product-service
    private Long supplierId; // Reference to Supplier in product-service
    private Long customerId; // Reference to User in auth-service
    
    private int quantity;
    private String stlFileUrl;
    
    @Column(columnDefinition = "TEXT")
    private String shippingAddress; // JSON string of ShippingAddress
    
    private LocalDateTime orderDate;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;
}
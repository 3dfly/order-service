package com.threedfly.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId; // Reference to User in auth-service
    private String businessName;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;
    private boolean verified;
    
    // Instead of direct references, we'll use IDs
    @ElementCollection
    private List<Long> productIds; // References to products in product-service
    
    @ElementCollection
    private List<Long> shopIds; // References to shops in product-service
    
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    private List<Order> orders;
} 
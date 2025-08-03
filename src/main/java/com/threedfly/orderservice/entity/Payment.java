package com.threedfly.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Column(nullable = false)
    private BigDecimal platformFee;
    
    @Column(nullable = false)
    private BigDecimal sellerAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;
    
    // Provider-agnostic fields (works for PayPal, Stripe, etc.)
    @Column(name = "provider_payment_id")
    private String providerPaymentId;
    
    @Column(name = "provider_payer_id") 
    private String providerPayerId;
    
    @Column(name = "platform_transaction_id")
    private String platformTransactionId;
    
    @Column(name = "seller_transaction_id")
    private String sellerTransactionId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @Column(length = 2000)
    private String providerResponse;
    
    // Store the raw request sent to the payment provider
    @Column(name = "raw_request", columnDefinition = "TEXT")
    private String rawRequest;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
} 
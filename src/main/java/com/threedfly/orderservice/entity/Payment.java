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
    
    // PayPal specific fields
    @Column(name = "paypal_payment_id")
    private String paypalPaymentId;
    
    @Column(name = "paypal_payer_id")
    private String paypalPayerId;
    
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
    private String paypalResponse;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
} 
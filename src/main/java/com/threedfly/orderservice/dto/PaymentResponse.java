package com.threedfly.orderservice.dto;

import com.threedfly.orderservice.entity.PaymentMethod;
import com.threedfly.orderservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long sellerId;
    private String sellerBusinessName;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BigDecimal sellerAmount;
    private PaymentStatus status;
    private PaymentMethod method;
    private String providerPaymentId;
    private String providerPayerId;
    private String platformTransactionId;
    private String sellerTransactionId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
} 
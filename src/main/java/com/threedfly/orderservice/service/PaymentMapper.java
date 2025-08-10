package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.PaymentResponse;
import com.threedfly.orderservice.entity.Order;
import com.threedfly.orderservice.entity.Payment;
import com.threedfly.orderservice.entity.PaymentStatus;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.service.payment.PaymentProviderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapper for Payment entities - handles clean creation and conversion
 * Uses builder pattern and validation for robust entity management
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMapper {

    @Value("${payment.platform.fee}")
    private BigDecimal platformFee;

    /**
     * Create a new Payment entity with proper validation and calculation
     */
    public Payment createPaymentEntity(CreatePaymentRequest request, Order order, Seller seller) {
        log.debug("🏗️ Creating payment entity for order: {} with total amount: ${}", 
                order.getId(), request.getTotalAmount());

        // Calculate supplier amount (total - platform fee)
        // Platform fee goes to 3dfly, remaining goes to supplier
        BigDecimal supplierAmount = request.getTotalAmount().subtract(platformFee);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setSeller(seller);
        payment.setTotalAmount(request.getTotalAmount());
        payment.setPlatformFee(platformFee);
        payment.setSellerAmount(supplierAmount); // This represents supplier amount
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(request.getMethod());
        payment.setCreatedAt(LocalDateTime.now());

        log.debug("✅ Created payment entity - Total: ${}, Platform Fee (3dfly): ${}, Supplier Amount: ${}", 
                payment.getTotalAmount(), payment.getPlatformFee(), payment.getSellerAmount());

        return payment;
    }

    /**
     * Update payment entity with provider result
     */
    public void updatePaymentWithProviderResult(Payment payment, PaymentProviderResult result) {
        if (result.isSuccess()) {
            payment.setStatus(result.getStatus());
            payment.setProviderPaymentId(result.getProviderPaymentId());
            payment.setProviderResponse(result.getProviderResponse());
            
            if (result.getPlatformTransactionId() != null) {
                payment.setPlatformTransactionId(result.getPlatformTransactionId());
            }
            if (result.getSellerTransactionId() != null) {
                payment.setSellerTransactionId(result.getSellerTransactionId());
            }
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                payment.setCompletedAt(LocalDateTime.now());
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(result.getErrorMessage());
        }
    }

    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .sellerId(payment.getSeller().getId())
                .sellerBusinessName(payment.getSeller().getBusinessName())
                .totalAmount(payment.getTotalAmount())
                .platformFee(payment.getPlatformFee())
                .sellerAmount(payment.getSellerAmount())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .providerPaymentId(payment.getProviderPaymentId())
                .providerPayerId(payment.getProviderPayerId())
                .platformTransactionId(payment.getPlatformTransactionId())
                .sellerTransactionId(payment.getSellerTransactionId())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }
} 
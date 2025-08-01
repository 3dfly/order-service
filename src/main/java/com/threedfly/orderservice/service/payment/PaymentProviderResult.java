package com.threedfly.orderservice.service.payment;

import com.threedfly.orderservice.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentProviderResult {
    private boolean success;
    private PaymentStatus status;
    private String providerPaymentId;
    private String approvalUrl;
    private String platformTransactionId;
    private String sellerTransactionId;
    private String errorMessage;
    private String providerResponse;

    public static PaymentProviderResult success(PaymentStatus status, String providerPaymentId, String approvalUrl) {
        return PaymentProviderResult.builder()
                .success(true)
                .status(status)
                .providerPaymentId(providerPaymentId)
                .approvalUrl(approvalUrl)
                .build();
    }

    public static PaymentProviderResult failure(String errorMessage) {
        return PaymentProviderResult.builder()
                .success(false)
                .status(PaymentStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
} 
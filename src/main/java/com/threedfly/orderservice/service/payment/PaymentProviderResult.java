package com.threedfly.orderservice.service.payment;

import com.threedfly.orderservice.entity.PaymentStatus;
import com.threedfly.orderservice.service.PaymentAuditService;
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
    
    // Raw request sent to payment provider for debugging
    private String rawRequest;
    
    // Audit data for HTTP request/response logging
    private PaymentAuditData auditData;

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
    
    /**
     * Wrapper for audit data containing HTTP request/response information
     */
    @Data
    @Builder
    public static class PaymentAuditData {
        private PaymentAuditService.HttpRequestData requestData;
        private PaymentAuditService.HttpResponseData responseData;
    }
} 
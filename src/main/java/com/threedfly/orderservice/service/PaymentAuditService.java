package com.threedfly.orderservice.service;

import com.threedfly.orderservice.entity.Payment;
import com.threedfly.orderservice.entity.PaymentAudit;
import com.threedfly.orderservice.entity.AuditAction;
import com.threedfly.orderservice.repository.PaymentAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAuditService {

    private final PaymentAuditRepository paymentAuditRepository;

    /**
     * Log HTTP request/response for payment operations
     */
    @Transactional
    public PaymentAudit logHttpRequest(Payment payment, AuditAction action, HttpRequestData requestData, HttpResponseData responseData) {
        try {
            PaymentAudit audit = PaymentAudit.builder()
                    .payment(payment)
                    .provider(payment.getMethod())
                    .action(action)
                    .requestUrl(requestData.getUrl())
                    .requestMethod(requestData.getMethod())
                    .requestHeaders(requestData.getHeaders())
                    .requestBody(requestData.getBody())
                    .responseStatus(responseData.getStatus())
                    .responseHeaders(responseData.getHeaders())
                    .responseBody(responseData.getBody())
                    .durationMs(responseData.getDurationMs())
                    .errorMessage(responseData.getErrorMessage())
                    .createdAt(LocalDateTime.now())
                    .build();

            audit = paymentAuditRepository.save(audit);
            
            log.debug("📋 Audit logged for payment {} - Action: {}, Status: {}, Duration: {}ms", 
                    payment.getId(), action, responseData.getStatus(), responseData.getDurationMs());
            
            return audit;
            
        } catch (Exception e) {
            log.error("❌ Failed to log audit for payment {}: {}", payment.getId(), e.getMessage());
            // Don't fail the main operation if audit logging fails
            return null;
        }
    }

    /**
     * Get all audit records for a payment
     */
    public List<PaymentAudit> getAuditHistory(Long paymentId) {
        return paymentAuditRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    /**
     * Get failed HTTP requests for monitoring
     */
    public List<PaymentAudit> getFailedRequests() {
        return paymentAuditRepository.findFailedRequests();
    }

    /**
     * Get audit records for a specific action
     */
    public List<PaymentAudit> getAuditsByAction(AuditAction action) {
        return paymentAuditRepository.findByAction(action);
    }

    /**
     * Data transfer objects for HTTP request/response data
     */
    public static class HttpRequestData {
        private String url;
        private String method;
        private String headers;
        private String body;

        public HttpRequestData(String url, String method, String headers, String body) {
            this.url = url;
            this.method = method;
            this.headers = headers;
            this.body = body;
        }

        // Getters
        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public String getHeaders() { return headers; }
        public String getBody() { return body; }
    }

    public static class HttpResponseData {
        private Integer status;
        private String headers;
        private String body;
        private Long durationMs;
        private String errorMessage;

        public HttpResponseData(Integer status, String headers, String body, Long durationMs, String errorMessage) {
            this.status = status;
            this.headers = headers;
            this.body = body;
            this.durationMs = durationMs;
            this.errorMessage = errorMessage;
        }

        // Getters
        public Integer getStatus() { return status; }
        public String getHeaders() { return headers; }
        public String getBody() { return body; }
        public Long getDurationMs() { return durationMs; }
        public String getErrorMessage() { return errorMessage; }
    }
} 
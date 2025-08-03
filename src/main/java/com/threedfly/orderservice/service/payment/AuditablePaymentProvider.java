package com.threedfly.orderservice.service.payment;

import com.threedfly.orderservice.service.PaymentAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * Base class demonstrating how to integrate audit logging into payment providers.
 * This shows the pattern that should be used in PayPalPaymentProvider, StripePaymentProvider, etc.
 */
@Slf4j
public abstract class AuditablePaymentProvider {

    /**
     * Helper method to create audit data from HTTP request/response
     * Use this pattern in your payment providers (PayPal, Stripe, etc.)
     */
    protected PaymentProviderResult.PaymentAuditData createAuditData(
            String url, 
            String method, 
            HttpHeaders requestHeaders, 
            String requestBody,
            ResponseEntity<String> response,
            long startTime) {

        long duration = System.currentTimeMillis() - startTime;

        // Create request data
        PaymentAuditService.HttpRequestData requestData = new PaymentAuditService.HttpRequestData(
                url,
                method,
                headersToString(requestHeaders),
                requestBody
        );

        // Create response data
        PaymentAuditService.HttpResponseData responseData = new PaymentAuditService.HttpResponseData(
                response.getStatusCode().value(),
                headersToString(response.getHeaders()),
                response.getBody(),
                duration,
                response.getStatusCode().isError() ? "HTTP Error: " + response.getStatusCode() : null
        );

        return PaymentProviderResult.PaymentAuditData.builder()
                .requestData(requestData)
                .responseData(responseData)
                .build();
    }

    /**
     * Helper method for error cases
     */
    protected PaymentProviderResult.PaymentAuditData createErrorAuditData(
            String url, 
            String method, 
            HttpHeaders requestHeaders, 
            String requestBody,
            Exception error,
            long startTime) {

        long duration = System.currentTimeMillis() - startTime;

        PaymentAuditService.HttpRequestData requestData = new PaymentAuditService.HttpRequestData(
                url,
                method,
                headersToString(requestHeaders),
                requestBody
        );

        PaymentAuditService.HttpResponseData responseData = new PaymentAuditService.HttpResponseData(
                null, // No status code for exceptions
                null,
                null,
                duration,
                error.getMessage()
        );

        return PaymentProviderResult.PaymentAuditData.builder()
                .requestData(requestData)
                .responseData(responseData)
                .build();
    }

    private String headersToString(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        headers.forEach((name, values) -> {
            // Mask sensitive headers
            if (name.toLowerCase().contains("authorization") || 
                name.toLowerCase().contains("x-api-key") ||
                name.toLowerCase().contains("cookie")) {
                sb.append(name).append(": [MASKED]\n");
            } else {
                sb.append(name).append(": ").append(String.join(", ", values)).append("\n");
            }
        });
        return sb.toString();
    }
}

/*
EXAMPLE USAGE IN PayPalPaymentProvider:

@Override
public PaymentProviderResult createPayment(Payment payment, CreatePaymentRequest request) {
    long startTime = System.currentTimeMillis();
    String url = paypalBaseUrl + "/v2/checkout/orders";
    String method = "POST";
    
    try {
        // Create PayPal request
        PayPalPaymentRequest paypalRequest = createPayPalRequest(payment);
        String requestBody = objectMapper.writeValueAsString(paypalRequest);
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Make HTTP call
        ResponseEntity<String> response = webClient.post()
                .uri(url)
                .headers(h -> h.addAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String.class)
                .block();
        
        // Parse response
        PayPalPaymentResponse paypalResponse = objectMapper.readValue(response.getBody(), PayPalPaymentResponse.class);
        
        // Create audit data
        PaymentProviderResult.PaymentAuditData auditData = createAuditData(
                url, method, headers, requestBody, response, startTime);
        
        return PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .providerPaymentId(paypalResponse.getId())
                .approvalUrl(getApprovalUrl(paypalResponse))
                .rawRequest(requestBody)  // Store raw request
                .auditData(auditData)     // Store audit data
                .build();
                
    } catch (Exception e) {
        log.error("❌ PayPal payment creation failed", e);
        
        PaymentProviderResult.PaymentAuditData auditData = createErrorAuditData(
                url, method, headers, requestBody, e, startTime);
        
        return PaymentProviderResult.builder()
                .success(false)
                .status(PaymentStatus.FAILED)
                .errorMessage(e.getMessage())
                .auditData(auditData)
                .build();
    }
}
*/ 
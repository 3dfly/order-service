package com.threedfly.orderservice.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.ExecutePaymentRequest;
import com.threedfly.orderservice.dto.paypal.PayPalPaymentRequest;
import com.threedfly.orderservice.dto.paypal.PayPalPaymentResponse;
import com.threedfly.orderservice.entity.Payment;
import com.threedfly.orderservice.entity.PaymentMethod;
import com.threedfly.orderservice.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service("paypal")
@RequiredArgsConstructor
@Slf4j
public class PayPalPaymentProvider implements PaymentProvider {

    private final WebClient paypalWebClient;
    private final ObjectMapper objectMapper;

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Override
    public boolean supports(PaymentMethod method) {
        return PaymentMethod.PAYPAL.equals(method);
    }

    @Override
    public PaymentProviderResult createPayment(Payment payment, CreatePaymentRequest request) {
        try {
            log.info("💳 Creating PayPal payment for amount: ${}", payment.getTotalAmount());

            // Get access token
            String accessToken = getAccessToken();
            
            // Build PayPal payment request using proper DTOs
            PayPalPaymentRequest paypalRequest = PayPalPaymentRequest.builder()
                    .intent("sale")
                    .payer(PayPalPaymentRequest.PayPalPayer.builder()
                            .paymentMethod("paypal")
                            .build())
                    .transactions(List.of(PayPalPaymentRequest.PayPalTransaction.builder()
                            .amount(PayPalPaymentRequest.PayPalAmount.builder()
                                    .total(payment.getTotalAmount().toString())
                                    .currency(request.getCurrency())
                                    .build())
                            .description(buildDescription(payment, request))
                            .build()))
                    .redirectUrls(PayPalPaymentRequest.PayPalRedirectUrls.builder()
                            .returnUrl(request.getSuccessUrl())
                            .cancelUrl(request.getCancelUrl())
                            .build())
                    .build();

            // Make API call
            String response = paypalWebClient.post()
                    .uri("/v1/payments/payment")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(paypalRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response using proper DTOs
            PayPalPaymentResponse paypalResponse = objectMapper.readValue(response, PayPalPaymentResponse.class);
            
            String approvalUrl = paypalResponse.getLinks().stream()
                    .filter(link -> "approval_url".equals(link.getRel()))
                    .map(PayPalPaymentResponse.PayPalLink::getHref)
                    .findFirst()
                    .orElse(null);

            return PaymentProviderResult.builder()
                    .success(true)
                    .status(PaymentStatus.PENDING)
                    .providerPaymentId(paypalResponse.getId())
                    .approvalUrl(approvalUrl)
                    .providerResponse(response)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to create PayPal payment", e);
            return PaymentProviderResult.failure("Failed to create PayPal payment: " + e.getMessage());
        }
    }

    @Override
    public PaymentProviderResult executePayment(Payment payment, ExecutePaymentRequest request) {
        try {
            log.info("✅ Executing PayPal payment: {}", request.getProviderPaymentId());

            String accessToken = getAccessToken();
            
            Map<String, Object> executeRequest = Map.of(
                "payer_id", request.getProviderPayerId()
            );

            String response = paypalWebClient.post()
                    .uri("/v1/payments/payment/{paymentId}/execute", request.getProviderPaymentId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(executeRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseNode = objectMapper.readTree(response);
            String state = responseNode.get("state").asText();
            
            PaymentStatus status = "approved".equals(state) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
            
            // Extract transaction IDs
            String platformTransactionId = extractTransactionId(responseNode);

            return PaymentProviderResult.builder()
                    .success(true)
                    .status(status)
                    .platformTransactionId(platformTransactionId)
                    .providerResponse(response)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to execute PayPal payment", e);
            return PaymentProviderResult.failure("Failed to execute PayPal payment: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "PayPal";
    }

    private String getAccessToken() {
        try {
            String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            
            String response = paypalWebClient.post()
                    .uri("/v1/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("grant_type=client_credentials")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseNode = objectMapper.readTree(response);
            return responseNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get PayPal access token: " + e.getMessage(), e);
        }
    }

    private String buildDescription(Payment payment, CreatePaymentRequest request) {
        return request.getDescription() != null ? 
                request.getDescription() : 
                "Order #" + payment.getOrder().getId() + " from " + payment.getSeller().getBusinessName();
    }

    private String extractTransactionId(JsonNode responseNode) {
        JsonNode transactions = responseNode.get("transactions");
        if (transactions != null && transactions.isArray() && transactions.size() > 0) {
            JsonNode relatedResources = transactions.get(0).get("related_resources");
            if (relatedResources != null && relatedResources.isArray() && relatedResources.size() > 0) {
                JsonNode sale = relatedResources.get(0).get("sale");
                if (sale != null) {
                    return sale.get("id").asText();
                }
            }
        }
        return null;
    }
} 
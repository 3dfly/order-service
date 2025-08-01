package com.threedfly.orderservice.dto.paypal;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PayPalPaymentRequest {
    private String intent;
    private PayPalPayer payer;
    private List<PayPalTransaction> transactions;
    private PayPalRedirectUrls redirectUrls;

    @Data
    @Builder
    public static class PayPalPayer {
        private String paymentMethod;
    }

    @Data
    @Builder
    public static class PayPalTransaction {
        private PayPalAmount amount;
        private String description;
    }

    @Data
    @Builder
    public static class PayPalAmount {
        private String total;
        private String currency;
    }

    @Data
    @Builder
    public static class PayPalRedirectUrls {
        private String returnUrl;
        private String cancelUrl;
    }
} 
package com.threedfly.orderservice.dto.paypal;

import lombok.Data;
import java.util.List;

@Data
public class PayPalPaymentResponse {
    private String id;
    private String state;
    private String createTime;
    private String updateTime;
    private List<PayPalLink> links;
    private List<PayPalTransaction> transactions;

    @Data
    public static class PayPalLink {
        private String href;
        private String rel;
        private String method;
    }

    @Data
    public static class PayPalTransaction {
        private List<PayPalRelatedResource> relatedResources;
    }

    @Data
    public static class PayPalRelatedResource {
        private PayPalSale sale;
    }

    @Data
    public static class PayPalSale {
        private String id;
        private String state;
        private String createTime;
        private String updateTime;
    }
} 
package com.threedfly.orderservice.dto.paypal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PayPalPayoutRequest {
    
    private PayPalSenderBatchHeader senderBatchHeader;
    private List<PayPalPayoutItem> items;
    
    @Data
    @Builder
    public static class PayPalSenderBatchHeader {
        private String senderBatchId;
        private String emailSubject;
        private String emailMessage;
    }
    
    @Data
    @Builder
    public static class PayPalPayoutItem {
        private String recipientType;
        private PayPalPayoutAmount amount;
        private String receiver;
        private String note;
        private String senderItemId;
    }
    
    @Data
    @Builder
    public static class PayPalPayoutAmount {
        private String value;
        private String currency;
    }
} 
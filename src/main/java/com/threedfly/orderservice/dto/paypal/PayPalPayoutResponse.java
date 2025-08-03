package com.threedfly.orderservice.dto.paypal;

import lombok.Data;

import java.util.List;

@Data
public class PayPalPayoutResponse {
    
    private PayPalBatchHeader batchHeader;
    private List<PayPalPayoutItemDetails> items;
    
    @Data
    public static class PayPalBatchHeader {
        private String payoutBatchId;
        private String batchStatus;
        private String timeCreated;
        private String timeCompleted;
        private PayPalPayoutAmount amount;
        private PayPalPayoutAmount fees;
    }
    
    @Data
    public static class PayPalPayoutItemDetails {
        private String payoutItemId;
        private String transactionId;
        private String transactionStatus;
        private PayPalPayoutAmount payoutItemFee;
        private PayPalPayoutAmount payoutBatchId;
        private String senderItemId;
    }
    
    @Data
    public static class PayPalPayoutAmount {
        private String value;
        private String currency;
    }
} 
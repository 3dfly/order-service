package com.threedfly.orderservice.entity;

public enum PaymentMethod {
    PAYPAL,
    STRIPE,
    CREDIT_CARD,
    BANK_TRANSFER,
    APPLE_PAY,
    GOOGLE_PAY;
    
    /**
     * Get the Spring bean name for the payment provider that handles this payment method
     */
    public String getProviderName() {
        return switch (this) {
            case PAYPAL -> "paypal";
            case STRIPE -> "stripe";
            case CREDIT_CARD -> "stripe"; // Credit cards handled by Stripe
            case BANK_TRANSFER -> "stripe"; // Bank transfers handled by Stripe
            case APPLE_PAY -> "stripe"; // Apple Pay handled by Stripe
            case GOOGLE_PAY -> "stripe"; // Google Pay handled by Stripe
        };
    }
} 
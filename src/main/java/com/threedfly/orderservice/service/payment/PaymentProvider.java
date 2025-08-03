package com.threedfly.orderservice.service.payment;

import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.ExecutePaymentRequest;
import com.threedfly.orderservice.entity.Payment;
import com.threedfly.orderservice.entity.PaymentMethod;

public interface PaymentProvider {
    
    /**
     * Check if this provider supports the given payment method
     */
    boolean supports(PaymentMethod method);
    
    /**
     * Create a payment with the provider (receive money from seller)
     */
    PaymentProviderResult createPayment(Payment payment, CreatePaymentRequest request);
    
    /**
     * Execute/complete a payment with the provider (complete receiving money from seller)
     */
    PaymentProviderResult executePayment(Payment payment, ExecutePaymentRequest request);
    
    /**
     * Send money to supplier after receiving payment from seller
     */
    PaymentProviderResult payoutToSupplier(Payment payment, String supplierPayoutAccount);
    
    /**
     * Get the provider name for logging/identification
     */
    String getProviderName();
} 
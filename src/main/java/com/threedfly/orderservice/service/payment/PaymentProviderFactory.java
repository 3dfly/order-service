package com.threedfly.orderservice.service.payment;

import com.threedfly.orderservice.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderFactory {

    private final ApplicationContext applicationContext;

    /**
     * Get the appropriate payment provider for the given payment method
     * @param method The payment method enum
     * @return The payment provider instance
     * @throws RuntimeException if no provider found for the payment method
     */
    public PaymentProvider getProvider(PaymentMethod method) {
        return getProvider(method.getProviderName());
    }

    /**
     * Get the appropriate payment provider for the given provider name
     * @param providerName The name of the payment provider (e.g., "paypal", "stripe")
     * @return The payment provider instance
     * @throws RuntimeException if no provider found with the given name
     */
    public PaymentProvider getProvider(String providerName) {
        try {
            PaymentProvider provider = applicationContext.getBean(providerName.toLowerCase(), PaymentProvider.class);
            log.info("🏭 Selected payment provider: {} for provider name: {}", provider.getProviderName(), providerName);
            return provider;
        } catch (Exception e) {
            log.error("❌ No payment provider found for name: {}", providerName);
            throw new RuntimeException("No payment provider found for method: " + providerName);
        }
    }
} 
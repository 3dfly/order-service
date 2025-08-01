package com.threedfly.orderservice.service.payment;

import com.threedfly.orderservice.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderFactory {

    private final List<PaymentProvider> paymentProviders;

    /**
     * Get the appropriate payment provider for the given payment method
     */
    public PaymentProvider getProvider(PaymentMethod method) {
        PaymentProvider provider = paymentProviders.stream()
                .filter(p -> p.supports(method))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No payment provider found for method: " + method));

        log.info("🏭 Selected payment provider: {} for method: {}", provider.getProviderName(), method);
        return provider;
    }

    /**
     * Get all supported payment methods
     */
    public List<PaymentMethod> getSupportedMethods() {
        return PaymentMethod.values().length > 0 ? 
                paymentProviders.stream()
                        .flatMap(provider -> List.of(PaymentMethod.values()).stream()
                                .filter(provider::supports))
                        .distinct()
                        .toList() : 
                List.of();
    }
} 
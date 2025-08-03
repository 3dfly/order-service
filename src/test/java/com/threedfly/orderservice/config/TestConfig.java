package com.threedfly.orderservice.config;

import com.threedfly.orderservice.service.PaymentAuditService;
import com.threedfly.orderservice.service.PaymentLockService;
import com.threedfly.orderservice.service.PaymentMapper;
import com.threedfly.orderservice.service.payment.PaymentProvider;
import com.threedfly.orderservice.service.payment.PaymentProviderFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public WebClient testPayPalWebClient() {
        return mock(WebClient.class);
    }

    @Bean
    @Primary
    public PaymentProviderFactory testPaymentProviderFactory() {
        PaymentProviderFactory mock = mock(PaymentProviderFactory.class);
        PaymentProvider mockProvider = testPaymentProvider();
        
        // Mock both method signatures to return the same mock provider
        when(mock.getProvider(any(com.threedfly.orderservice.entity.PaymentMethod.class))).thenReturn(mockProvider);
        when(mock.getProvider(any(String.class))).thenReturn(mockProvider);
        
        return mock;
    }

    @Bean
    @Primary
    public PaymentMapper testPaymentMapper() {
        return mock(PaymentMapper.class);
    }

    @Bean
    @Primary
    public PaymentLockService testPaymentLockService() {
        return mock(PaymentLockService.class);
    }

    @Bean
    @Primary
    public PaymentProvider testPaymentProvider() {
        return mock(PaymentProvider.class);
    }

    @Bean
    @Primary
    public PaymentAuditService testPaymentAuditService() {
        return mock(PaymentAuditService.class);
    }
} 
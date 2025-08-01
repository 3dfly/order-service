package com.threedfly.orderservice.config;

import com.threedfly.orderservice.service.PaymentLockService;
import com.threedfly.orderservice.service.PaymentMapper;
import com.threedfly.orderservice.service.payment.PaymentProvider;
import com.threedfly.orderservice.service.payment.PaymentProviderFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.mock;

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
        return mock(PaymentProviderFactory.class);
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
} 
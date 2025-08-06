package com.threedfly.orderservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class PayPalConfig {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Value("${paypal.base.url:https://sandbox.paypal.com}")
    private String baseUrl;

    @Bean
    public WebClient payPalWebClient() {
        log.info("🔧 Initializing PayPal WebClient for {} mode with base URL: {}", mode, baseUrl);
        
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Language", "en_US")
                .build();
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getMode() {
        return mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
} 
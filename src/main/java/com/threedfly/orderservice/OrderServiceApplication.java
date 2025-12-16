package com.threedfly.orderservice;

import com.threedfly.orderservice.config.PrintingPricingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PrintingPricingConfig.class)
public class OrderServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);

	public static void main(String[] args) {
		logger.info("🚀 Starting Order Service Application...");
		SpringApplication.run(OrderServiceApplication.class, args);
		logger.info("✅ Order Service Application started successfully!");
	}

}

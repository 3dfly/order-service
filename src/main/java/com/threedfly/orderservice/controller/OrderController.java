package com.threedfly.orderservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @GetMapping
    public ResponseEntity<String> getOrders() {
        logger.info("📋 GET /orders endpoint called");
        try {
            String response = "Here are your orders!";
            logger.info("✅ GET /orders completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error in GET /orders", e);
            throw e;
        }
    }

    @GetMapping("/calculate")
    public ResponseEntity<String> calculateOrders() {
        logger.info("🧮 GET /orders/calculate endpoint called");
        try {
            String response = "Calculating order summary...";
            logger.info("✅ GET /orders/calculate completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error in GET /orders/calculate", e);
            throw e;
        }
    }
}

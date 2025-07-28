package com.threedfly.orderservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired(required = false)
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("💚 Health check endpoint called");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "order-service");
        
        // Check database connection
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    health.put("database", "UP");
                    logger.info("✅ Database connection is healthy");
                } else {
                    health.put("database", "DOWN");
                    logger.warn("⚠️ Database connection validation failed");
                }
            } catch (Exception e) {
                health.put("database", "DOWN");
                health.put("database_error", e.getMessage());
                logger.error("❌ Database connection failed", e);
            }
        } else {
            health.put("database", "NOT_CONFIGURED");
            logger.warn("⚠️ DataSource is not available");
        }
        
        logger.info("💚 Health check completed: {}", health.get("status"));
        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        logger.info("🔍 Readiness check endpoint called");
        
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("ready", true);
        readiness.put("timestamp", LocalDateTime.now().toString());
        
        logger.info("✅ Readiness check passed");
        return ResponseEntity.ok(readiness);
    }
} 
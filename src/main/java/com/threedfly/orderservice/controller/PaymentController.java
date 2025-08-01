package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.ExecutePaymentRequest;
import com.threedfly.orderservice.dto.PaymentResponse;
import com.threedfly.orderservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for payment operations with automatic splitting
 * Supports PayPal integration with extensible provider pattern
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment with automatic revenue splitting
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("💳 POST /payments - Creating payment for order: {} with method: {}", 
                request.getOrderId(), request.getMethod());
        try {
            PaymentResponse response = paymentService.createPayment(request);
            log.info("✅ Payment created successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("❌ Error creating payment", e);
            throw e;
        }
    }

    /**
     * Execute a payment (complete PayPal payment after approval)
     * THREAD-SAFE: Uses locking to prevent concurrent execution
     */
    @PostMapping("/{paymentId}/execute")
    public ResponseEntity<PaymentResponse> executePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody ExecutePaymentRequest request) {
        log.info("✅ POST /payments/{}/execute - Executing payment", paymentId);
        try {
            PaymentResponse response = paymentService.executePayment(request);
            log.info("✅ Payment executed successfully: {}", response.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error executing payment", e);
            throw e;
        }
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        log.info("🔍 GET /payments/{} - Retrieving payment by ID", id);
        try {
            PaymentResponse response = paymentService.getPaymentById(id);
            log.info("✅ Retrieved payment: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error retrieving payment by ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Get payments by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrder(@PathVariable Long orderId) {
        log.info("📋 GET /payments/order/{} - Retrieving payments by order", orderId);
        try {
            List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(orderId);
            log.info("✅ Retrieved {} payments for order: {}", payments.size(), orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("❌ Error retrieving payments for order: {}", orderId, e);
            throw e;
        }
    }

    /**
     * Get payments by seller ID
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsBySeller(@PathVariable Long sellerId) {
        log.info("🏪 GET /payments/seller/{} - Retrieving payments by seller", sellerId);
        try {
            List<PaymentResponse> payments = paymentService.getPaymentsBySellerId(sellerId);
            log.info("✅ Retrieved {} payments for seller: {}", payments.size(), sellerId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("❌ Error retrieving payments for seller: {}", sellerId, e);
            throw e;
        }
    }

    /**
     * Handle PayPal webhook events
     */
    @PostMapping("/webhook/paypal")
    public ResponseEntity<String> handlePayPalWebhook(@RequestBody String payload) {
        log.info("🔔 PayPal webhook received");
        try {
            paymentService.handlePaymentWebhook(payload, "PayPal");
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            log.error("❌ Error processing PayPal webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }
} 
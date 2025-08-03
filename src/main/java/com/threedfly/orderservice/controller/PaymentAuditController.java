package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.entity.PaymentAudit;
import com.threedfly.orderservice.entity.AuditAction;
import com.threedfly.orderservice.service.PaymentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments/audit")
@RequiredArgsConstructor
@Slf4j
public class PaymentAuditController {

    private final PaymentAuditService paymentAuditService;

    /**
     * Get audit history for a specific payment
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<PaymentAudit>> getPaymentAuditHistory(@PathVariable Long paymentId) {
        log.info("📋 Retrieving audit history for payment: {}", paymentId);
        List<PaymentAudit> audits = paymentAuditService.getAuditHistory(paymentId);
        return ResponseEntity.ok(audits);
    }

    /**
     * Get failed HTTP requests for monitoring
     */
    @GetMapping("/failed")
    public ResponseEntity<List<PaymentAudit>> getFailedRequests() {
        log.info("🚨 Retrieving failed payment requests for monitoring");
        List<PaymentAudit> failedRequests = paymentAuditService.getFailedRequests();
        return ResponseEntity.ok(failedRequests);
    }

    /**
     * Get audit records for a specific action type
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<List<PaymentAudit>> getAuditsByAction(@PathVariable AuditAction action) {
        log.info("🔍 Retrieving audit records for action: {}", action);
        List<PaymentAudit> audits = paymentAuditService.getAuditsByAction(action);
        return ResponseEntity.ok(audits);
    }
} 
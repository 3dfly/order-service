package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.dto.CreateSellerRequest;
import com.threedfly.orderservice.dto.SellerResponse;
import com.threedfly.orderservice.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
@Slf4j
public class SellerController {

    private final SellerService sellerService;

    @PostMapping
    public ResponseEntity<SellerResponse> createSeller(@Valid @RequestBody CreateSellerRequest request) {
        log.info("📝 POST /sellers - Creating new seller for user: {}", request.getUserId());
        try {
            SellerResponse response = sellerService.createSeller(request);
            log.info("✅ Seller created successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("❌ Error creating seller", e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<SellerResponse>> getAllSellers() {
        log.info("📋 GET /sellers - Retrieving all sellers");
        try {
            List<SellerResponse> sellers = sellerService.getAllSellers();
            log.info("✅ Retrieved {} sellers successfully", sellers.size());
            return ResponseEntity.ok(sellers);
        } catch (Exception e) {
            log.error("❌ Error retrieving sellers", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SellerResponse> getSellerById(@PathVariable Long id) {
        log.info("🔍 GET /sellers/{} - Retrieving seller by ID", id);
        try {
            SellerResponse seller = sellerService.getSellerById(id);
            log.info("✅ Seller retrieved successfully: {}", id);
            return ResponseEntity.ok(seller);
        } catch (Exception e) {
            log.error("❌ Error retrieving seller with ID: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SellerResponse> getSellerByUserId(@PathVariable Long userId) {
        log.info("👤 GET /sellers/user/{} - Retrieving seller by user ID", userId);
        try {
            SellerResponse seller = sellerService.getSellerByUserId(userId);
            log.info("✅ Seller retrieved successfully for user: {}", userId);
            return ResponseEntity.ok(seller);
        } catch (Exception e) {
            log.error("❌ Error retrieving seller for user: {}", userId, e);
            throw e;
        }
    }

    @GetMapping("/verified")
    public ResponseEntity<List<SellerResponse>> getVerifiedSellers() {
        log.info("✅ GET /sellers/verified - Retrieving verified sellers");
        try {
            List<SellerResponse> sellers = sellerService.getVerifiedSellers();
            log.info("✅ Retrieved {} verified sellers", sellers.size());
            return ResponseEntity.ok(sellers);
        } catch (Exception e) {
            log.error("❌ Error retrieving verified sellers", e);
            throw e;
        }
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<SellerResponse> verifySeller(@PathVariable Long id) {
        log.info("✅ PATCH /sellers/{}/verify - Verifying seller", id);
        try {
            SellerResponse response = sellerService.verifySeller(id);
            log.info("✅ Seller verified successfully: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error verifying seller: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeller(@PathVariable Long id) {
        log.info("🗑️ DELETE /sellers/{} - Deleting seller", id);
        try {
            sellerService.deleteSeller(id);
            log.info("✅ Seller deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ Error deleting seller: {}", id, e);
            throw e;
        }
    }
} 
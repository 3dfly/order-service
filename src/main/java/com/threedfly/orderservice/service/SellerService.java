package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.CreateSellerRequest;
import com.threedfly.orderservice.dto.SellerResponse;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SellerService {
    
    private final SellerRepository sellerRepository;
    
    public SellerResponse createSeller(CreateSellerRequest request) {
        log.info("Creating new seller for user: {}", request.getUserId());
        
        // Check if seller already exists for this user
        if (sellerRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("Seller already exists for user ID: " + request.getUserId());
        }
        
        // Create seller entity
        Seller seller = new Seller();
        seller.setUserId(request.getUserId());
        seller.setBusinessName(request.getBusinessName());
        seller.setBusinessAddress(request.getBusinessAddress());
        seller.setContactPhone(request.getContactPhone());
        seller.setContactEmail(request.getContactEmail());
        seller.setVerified(false); // Default to unverified
        seller.setProductIds(request.getProductIds());
        seller.setShopIds(request.getShopIds());
        
        Seller savedSeller = sellerRepository.save(seller);
        log.info("Seller created successfully with ID: {}", savedSeller.getId());
        
        return convertToSellerResponse(savedSeller);
    }
    
    @Transactional(readOnly = true)
    public List<SellerResponse> getAllSellers() {
        log.info("Retrieving all sellers");
        return sellerRepository.findAll().stream()
                .map(this::convertToSellerResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SellerResponse getSellerById(Long id) {
        log.info("Retrieving seller with ID: {}", id);
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + id));
        return convertToSellerResponse(seller);
    }
    
    @Transactional(readOnly = true)
    public SellerResponse getSellerByUserId(Long userId) {
        log.info("Retrieving seller for user: {}", userId);
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found for user ID: " + userId));
        return convertToSellerResponse(seller);
    }
    
    @Transactional(readOnly = true)
    public List<SellerResponse> getVerifiedSellers() {
        log.info("Retrieving verified sellers");
        return sellerRepository.findByVerified(true).stream()
                .map(this::convertToSellerResponse)
                .collect(Collectors.toList());
    }
    
    public SellerResponse verifySeller(Long id) {
        log.info("Verifying seller with ID: {}", id);
        
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + id));
        
        seller.setVerified(true);
        Seller updatedSeller = sellerRepository.save(seller);
        
        log.info("Seller verified successfully with ID: {}", id);
        return convertToSellerResponse(updatedSeller);
    }
    
    public void deleteSeller(Long id) {
        log.info("Deleting seller with ID: {}", id);
        
        if (!sellerRepository.existsById(id)) {
            throw new RuntimeException("Seller not found with ID: " + id);
        }
        
        sellerRepository.deleteById(id);
        log.info("Seller deleted successfully with ID: {}", id);
    }
    
    private SellerResponse convertToSellerResponse(Seller seller) {
        SellerResponse response = new SellerResponse();
        response.setId(seller.getId());
        response.setUserId(seller.getUserId());
        response.setBusinessName(seller.getBusinessName());
        response.setBusinessAddress(seller.getBusinessAddress());
        response.setContactPhone(seller.getContactPhone());
        response.setContactEmail(seller.getContactEmail());
        response.setVerified(seller.isVerified());
        response.setProductIds(seller.getProductIds());
        response.setShopIds(seller.getShopIds());
        return response;
    }
} 
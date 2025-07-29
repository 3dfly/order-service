package com.threedfly.orderservice.repository;

import com.threedfly.orderservice.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    // Find seller by user ID
    Optional<Seller> findByUserId(Long userId);
    
    // Find verified sellers
    List<Seller> findByVerified(boolean verified);
    
    // Find sellers by business name (case insensitive)
    List<Seller> findByBusinessNameContainingIgnoreCase(String businessName);
    
    // Find seller by contact email
    Optional<Seller> findByContactEmail(String contactEmail);
    
    // Check if seller exists by user ID
    boolean existsByUserId(Long userId);
} 
package com.threedfly.orderservice.repository;

import com.threedfly.orderservice.entity.Payment;
import com.threedfly.orderservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByOrderId(Long orderId);
    
    List<Payment> findBySellerId(Long sellerId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    
    @Query("SELECT p FROM Payment p WHERE p.seller.id = :sellerId AND p.status = :status")
    List<Payment> findBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.platformFee) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalPlatformFeesForPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.sellerAmount) FROM Payment p WHERE p.seller.id = :sellerId AND p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalSellerEarningsForPeriod(@Param("sellerId") Long sellerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 
package com.threedfly.orderservice.repository;

import com.threedfly.orderservice.entity.PaymentAudit;
import com.threedfly.orderservice.entity.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {
    
    List<PaymentAudit> findByPaymentId(Long paymentId);
    
    List<PaymentAudit> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
    
    List<PaymentAudit> findByAction(AuditAction action);
    
    @Query("SELECT pa FROM PaymentAudit pa WHERE pa.payment.id = :paymentId AND pa.action = :action ORDER BY pa.createdAt DESC")
    List<PaymentAudit> findByPaymentIdAndAction(@Param("paymentId") Long paymentId, @Param("action") AuditAction action);
    
    @Query("SELECT pa FROM PaymentAudit pa WHERE pa.createdAt BETWEEN :startDate AND :endDate ORDER BY pa.createdAt DESC")
    List<PaymentAudit> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pa FROM PaymentAudit pa WHERE pa.responseStatus >= 400 ORDER BY pa.createdAt DESC")
    List<PaymentAudit> findFailedRequests();
} 
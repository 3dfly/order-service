package com.threedfly.orderservice.repository;

import com.threedfly.orderservice.entity.Order;
import com.threedfly.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find orders by customer ID
    List<Order> findByCustomerId(Long customerId);
    
    // Find orders by seller ID
    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId")
    List<Order> findBySellerId(@Param("sellerId") Long sellerId);
    
    // Find orders by status
    List<Order> findByStatus(OrderStatus status);
    
    // Find orders by customer ID and status
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
    
    // Find orders by date range
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find orders by product ID
    List<Order> findByProductId(String productId);
    
    // Find orders by supplier ID
    List<Order> findBySupplierId(Long supplierId);
}
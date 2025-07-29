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
    List<Order> findBySellerId(Long sellerId);
    
    // Find orders by status
    List<Order> findByStatus(OrderStatus status);
    
    // Find orders by customer ID and status
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
    
    // Find orders by date range
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find orders by product ID
    List<Order> findByProductId(Long productId);
    
    // Find orders by supplier ID
    List<Order> findBySupplierId(Long supplierId);
    
    // Custom query to get orders with total value greater than specified amount
    @Query("SELECT o FROM Order o WHERE o.totalPrice > :minPrice")
    List<Order> findOrdersWithTotalPriceGreaterThan(@Param("minPrice") double minPrice);
    
    // Custom query to get order statistics by customer
    @Query("SELECT COUNT(o), SUM(o.totalPrice) FROM Order o WHERE o.customerId = :customerId")
    Object[] getOrderStatisticsByCustomer(@Param("customerId") Long customerId);
} 
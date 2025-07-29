package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.*;
import com.threedfly.orderservice.entity.Order;
import com.threedfly.orderservice.entity.OrderStatus;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.repository.OrderRepository;
import com.threedfly.orderservice.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;

    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerId());
        
        // Find seller
        Seller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + request.getSellerId()));
        
        // Create order entity
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setSupplierId(request.getSupplierId());
        order.setCustomerId(request.getCustomerId());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(request.getTotalPrice());
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setSeller(seller);
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        
        return convertToOrderResponse(savedOrder);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Retrieving all orders");
        return orderRepository.findAll().stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Retrieving order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        return convertToOrderResponse(order);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        log.info("Retrieving orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        log.info("Retrieving orders with status: {}", status);
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }
    
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        log.info("Updating order with ID: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        
        // Update fields if provided
        if (request.getQuantity() != null) {
            order.setQuantity(request.getQuantity());
        }
        if (request.getTotalPrice() != null) {
            order.setTotalPrice(request.getTotalPrice());
        }
        if (request.getCustomerName() != null) {
            order.setCustomerName(request.getCustomerName());
        }
        if (request.getCustomerEmail() != null) {
            order.setCustomerEmail(request.getCustomerEmail());
        }
        if (request.getShippingAddress() != null) {
            order.setShippingAddress(request.getShippingAddress());
        }
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated successfully with ID: {}", updatedOrder.getId());
        
        return convertToOrderResponse(updatedOrder);
    }
    
    public void deleteOrder(Long id) {
        log.info("Deleting order with ID: {}", id);
        
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with ID: " + id);
        }
        
        orderRepository.deleteById(id);
        log.info("Order deleted successfully with ID: {}", id);
    }
    
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        log.info("Updating order status for ID: {} to {}", id, status);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order status updated successfully for ID: {}", id);
        return convertToOrderResponse(updatedOrder);
    }
    
    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setProductId(order.getProductId());
        response.setSupplierId(order.getSupplierId());
        response.setCustomerId(order.getCustomerId());
        response.setSellerId(order.getSeller().getId());
        response.setQuantity(order.getQuantity());
        response.setTotalPrice(order.getTotalPrice());
        response.setOrderDate(order.getOrderDate());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setShippingAddress(order.getShippingAddress());
        response.setStatus(order.getStatus());
        
        // Convert seller to response
        if (order.getSeller() != null) {
            SellerResponse sellerResponse = new SellerResponse();
            sellerResponse.setId(order.getSeller().getId());
            sellerResponse.setUserId(order.getSeller().getUserId());
            sellerResponse.setBusinessName(order.getSeller().getBusinessName());
            sellerResponse.setBusinessAddress(order.getSeller().getBusinessAddress());
            sellerResponse.setContactPhone(order.getSeller().getContactPhone());
            sellerResponse.setContactEmail(order.getSeller().getContactEmail());
            sellerResponse.setVerified(order.getSeller().isVerified());
            sellerResponse.setProductIds(order.getSeller().getProductIds());
            sellerResponse.setShopIds(order.getSeller().getShopIds());
            response.setSeller(sellerResponse);
        }
        
        return response;
    }
} 
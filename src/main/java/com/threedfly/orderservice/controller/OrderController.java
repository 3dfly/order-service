package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.dto.*;
import com.threedfly.orderservice.entity.OrderStatus;
import com.threedfly.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("📝 POST /orders - Creating new order for customer: {}", request.getCustomerId());
        try {
            OrderResponse response = orderService.createOrder(request);
            log.info("✅ Order created successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("❌ Error creating order", e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("📋 GET /orders - Retrieving all orders");
        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            log.info("✅ Retrieved {} orders successfully", orders.size());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Error retrieving orders", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("🔍 GET /orders/{} - Retrieving order by ID", id);
        try {
            OrderResponse order = orderService.getOrderById(id);
            log.info("✅ Order retrieved successfully: {}", id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("❌ Error retrieving order with ID: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        log.info("👤 GET /orders/customer/{} - Retrieving orders by customer", customerId);
        try {
            List<OrderResponse> orders = orderService.getOrdersByCustomerId(customerId);
            log.info("✅ Retrieved {} orders for customer: {}", orders.size(), customerId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Error retrieving orders for customer: {}", customerId, e);
            throw e;
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        log.info("📊 GET /orders/status/{} - Retrieving orders by status", status);
        try {
            List<OrderResponse> orders = orderService.getOrdersByStatus(status);
            log.info("✅ Retrieved {} orders with status: {}", orders.size(), status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Error retrieving orders with status: {}", status, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        log.info("📝 PUT /orders/{} - Updating order", id);
        try {
            OrderResponse response = orderService.updateOrder(id, request);
            log.info("✅ Order updated successfully: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error updating order: {}", id, e);
            throw e;
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        log.info("🔄 PATCH /orders/{}/status - Updating order status to: {}", id, status);
        try {
            OrderResponse response = orderService.updateOrderStatus(id, status);
            log.info("✅ Order status updated successfully: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error updating order status: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("🗑️ DELETE /orders/{} - Deleting order", id);
        try {
            orderService.deleteOrder(id);
            log.info("✅ Order deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ Error deleting order: {}", id, e);
            throw e;
        }
    }
}

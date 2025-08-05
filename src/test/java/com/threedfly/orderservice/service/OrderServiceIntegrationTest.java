package com.threedfly.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.TestUtils;
import com.threedfly.orderservice.dto.CreateOrderRequest;
import com.threedfly.orderservice.dto.OrderResponse;
import com.threedfly.orderservice.dto.ShippingAddress;
import com.threedfly.orderservice.dto.UpdateOrderRequest;
import com.threedfly.orderservice.entity.Order;
import com.threedfly.orderservice.entity.OrderStatus;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.repository.OrderRepository;
import com.threedfly.orderservice.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest validOrderRequest;
    private Seller testSeller;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        orderRepository.deleteAll();
        sellerRepository.deleteAll();

        // Create test seller
        testSeller = new Seller();
        testSeller.setUserId(1001L);
        testSeller.setBusinessName("Test Store");
        testSeller.setBusinessAddress("123 Test St");
        testSeller.setContactEmail("test@store.com");
        testSeller.setContactPhone("+15550123");
        testSeller.setVerified(true);
        testSeller = sellerRepository.save(testSeller);

        // Create valid order request
        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId(2001L);
        validOrderRequest.setProductId("PROD-1001");
        validOrderRequest.setQuantity(2);
        validOrderRequest.setStlFileUrl("https://example.com/model.stl");
        validOrderRequest.setShippingAddress(TestUtils.createTestShippingAddress());
        validOrderRequest.setSupplierId(3001L);
        validOrderRequest.setSellerId(testSeller.getId());
    }

    @Test
    void testCreateOrder_Success() {
        // Act
        OrderResponse response = orderService.createOrder(validOrderRequest);

        // Assert
        assertNotNull(response);
        assertEquals(validOrderRequest.getCustomerId(), response.getCustomerId());
        assertEquals(validOrderRequest.getProductId(), response.getProductId());
        assertEquals(validOrderRequest.getQuantity(), response.getQuantity());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertNotNull(response.getOrderDate());
    }

    @Test
    void testCreateOrder_DuplicateOrder() {
        // Act
        OrderResponse firstOrder = orderService.createOrder(validOrderRequest);
        
        // Create second order with same details
        CreateOrderRequest secondRequest = new CreateOrderRequest();
        secondRequest.setCustomerId(2002L);
        secondRequest.setProductId("PROD-1002");
        secondRequest.setQuantity(1);
        secondRequest.setStlFileUrl("https://example.com/another-model.stl");
        secondRequest.setShippingAddress(TestUtils.createTestShippingAddress());
        secondRequest.setSupplierId(3001L);
        secondRequest.setSellerId(testSeller.getId());

        OrderResponse secondOrder = orderService.createOrder(secondRequest);

        // Assert
        assertNotEquals(firstOrder.getId(), secondOrder.getId());
    }

    @Test
    void testGetOrderById_Success() {
        // Arrange
        OrderResponse created = orderService.createOrder(validOrderRequest);

        // Act
        OrderResponse retrieved = orderService.getOrderById(created.getId());

        // Assert
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getCustomerId(), retrieved.getCustomerId());
        assertEquals(created.getProductId(), retrieved.getProductId());
    }

    @Test
    void testGetOrdersByCustomer_Success() {
        // Create first order
        orderService.createOrder(validOrderRequest);

        // Create second order for different customer
        CreateOrderRequest differentCustomer = new CreateOrderRequest();
        differentCustomer.setCustomerId(2002L);
        differentCustomer.setProductId("PROD-1002");
        differentCustomer.setQuantity(1);
        differentCustomer.setStlFileUrl("https://example.com/another-model.stl");
        differentCustomer.setShippingAddress(TestUtils.createTestShippingAddress());
        differentCustomer.setSupplierId(3001L);
        differentCustomer.setSellerId(testSeller.getId());
        orderService.createOrder(differentCustomer);

        // Get orders for first customer
        List<OrderResponse> customerOrders = orderService.getOrdersByCustomerId(validOrderRequest.getCustomerId());

        // Assert
        assertEquals(1, customerOrders.size());
        assertEquals(validOrderRequest.getCustomerId(), customerOrders.get(0).getCustomerId());
    }

    @Test
    void testGetOrdersByStatus_Success() {
        // Create first order (PENDING by default)
        orderService.createOrder(validOrderRequest);

        // Create second order and update its status
        CreateOrderRequest secondRequest = new CreateOrderRequest();
        secondRequest.setCustomerId(2002L);
        secondRequest.setProductId("PROD-1002");
        secondRequest.setQuantity(1);
        secondRequest.setStlFileUrl("https://example.com/another-model.stl");
        secondRequest.setShippingAddress(TestUtils.createTestShippingAddress());
        secondRequest.setSupplierId(3001L);
        secondRequest.setSellerId(testSeller.getId());
        
        OrderResponse secondOrder = orderService.createOrder(secondRequest);
        orderService.updateOrderStatus(secondOrder.getId(), OrderStatus.PROCESSING);

        // Get PENDING orders
        List<OrderResponse> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);

        // Assert
        assertEquals(1, pendingOrders.size());
        assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
    }

    @Test
    void testUpdateOrder_Success() {
        // Create order
        OrderResponse created = orderService.createOrder(validOrderRequest);

        // Update order
        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(5);
        updateRequest.setStlFileUrl("https://example.com/updated-model.stl");
        updateRequest.setShippingAddress(TestUtils.createTestShippingAddress());

        // Act
        OrderResponse updated = orderService.updateOrder(created.getId(), updateRequest);

        // Assert
        assertEquals(5, updated.getQuantity());
        assertEquals("https://example.com/updated-model.stl", updated.getStlFileUrl());
        assertNotNull(updated.getShippingAddress());
    }
}
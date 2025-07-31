package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.CreateOrderRequest;
import com.threedfly.orderservice.dto.OrderResponse;
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

import java.time.LocalDateTime;
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

    private Seller testSeller;
    private CreateOrderRequest validOrderRequest;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        sellerRepository.deleteAll();

        // Create a test seller
        testSeller = new Seller();
        testSeller.setUserId(1001L);
        testSeller.setBusinessName("Test Store");
        testSeller.setBusinessAddress("123 Test St");
        testSeller.setContactEmail("test@store.com");
        testSeller.setContactPhone("+1-555-0123");
        testSeller.setVerified(true);
        testSeller = sellerRepository.save(testSeller);

        // Create valid order request
        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId(2001L);
        validOrderRequest.setCustomerName("John Doe");
        validOrderRequest.setCustomerEmail("john@example.com");
        validOrderRequest.setProductId(1001L);
        validOrderRequest.setQuantity(2);
        validOrderRequest.setTotalPrice(199.99);
        validOrderRequest.setShippingAddress("456 Customer Ave, City");
        validOrderRequest.setSupplierId(3001L);
        validOrderRequest.setSellerId(testSeller.getId());
    }

    @Test
    void testCreateOrder_Success() {
        OrderResponse response = orderService.createOrder(validOrderRequest);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(validOrderRequest.getCustomerId(), response.getCustomerId());
        assertEquals(validOrderRequest.getCustomerName(), response.getCustomerName());
        assertEquals(validOrderRequest.getProductId(), response.getProductId());
        assertEquals(validOrderRequest.getQuantity(), response.getQuantity());
        assertEquals(validOrderRequest.getTotalPrice(), response.getTotalPrice());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertNotNull(response.getOrderDate());
    }

    @Test
    void testGetAllOrders_Success() {
        // Create multiple orders
        orderService.createOrder(validOrderRequest);
        
        CreateOrderRequest secondRequest = new CreateOrderRequest();
        secondRequest.setCustomerId(2002L);
        secondRequest.setCustomerName("Jane Smith");
        secondRequest.setCustomerEmail("jane@example.com");
        secondRequest.setProductId(1002L);
        secondRequest.setQuantity(1);
        secondRequest.setTotalPrice(99.99);
        secondRequest.setShippingAddress("789 Another St, City");
        secondRequest.setSupplierId(3001L);
        secondRequest.setSellerId(testSeller.getId());
        orderService.createOrder(secondRequest);

        List<OrderResponse> orders = orderService.getAllOrders();

        assertEquals(2, orders.size());
    }

    @Test
    void testGetOrderById_Success() {
        OrderResponse created = orderService.createOrder(validOrderRequest);
        
        OrderResponse retrieved = orderService.getOrderById(created.getId());

        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getCustomerName(), retrieved.getCustomerName());
    }

    @Test
    void testGetOrderById_NotFound() {
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(99999L);
        });
    }

    @Test
    void testGetOrdersByCustomerId_Success() {
        // Create orders for different customers
        orderService.createOrder(validOrderRequest);
        
        CreateOrderRequest differentCustomer = new CreateOrderRequest();
        differentCustomer.setCustomerId(2002L);
        differentCustomer.setCustomerName("Jane Smith");
        differentCustomer.setCustomerEmail("jane@example.com");
        differentCustomer.setProductId(1002L);
        differentCustomer.setQuantity(1);
        differentCustomer.setTotalPrice(99.99);
        differentCustomer.setShippingAddress("789 Another St, City");
        differentCustomer.setSupplierId(3001L);
        differentCustomer.setSellerId(testSeller.getId());
        orderService.createOrder(differentCustomer);

        List<OrderResponse> customerOrders = orderService.getOrdersByCustomerId(2001L);

        assertEquals(1, customerOrders.size());
        assertEquals(2001L, customerOrders.get(0).getCustomerId());
    }

    @Test
    void testGetOrdersByStatus_Success() {
        // Create orders with different statuses
        OrderResponse order1 = orderService.createOrder(validOrderRequest);
        
        CreateOrderRequest secondRequest = new CreateOrderRequest();
        secondRequest.setCustomerId(2002L);
        secondRequest.setCustomerName("Jane Smith");
        secondRequest.setCustomerEmail("jane@example.com");
        secondRequest.setProductId(1002L);
        secondRequest.setQuantity(1);
        secondRequest.setTotalPrice(99.99);
        secondRequest.setShippingAddress("789 Another St, City");
        secondRequest.setSupplierId(3001L);
        secondRequest.setSellerId(testSeller.getId());
        OrderResponse order2 = orderService.createOrder(secondRequest);

        // Update one order status
        orderService.updateOrderStatus(order2.getId(), OrderStatus.PROCESSING);

        List<OrderResponse> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        List<OrderResponse> processingOrders = orderService.getOrdersByStatus(OrderStatus.PROCESSING);

        assertEquals(1, pendingOrders.size());
        assertEquals(1, processingOrders.size());
        assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
        assertEquals(OrderStatus.PROCESSING, processingOrders.get(0).getStatus());
    }

    @Test
    void testUpdateOrder_Success() {
        OrderResponse created = orderService.createOrder(validOrderRequest);

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(5);
        updateRequest.setTotalPrice(299.99);
        updateRequest.setShippingAddress("Updated Address");

        OrderResponse updated = orderService.updateOrder(created.getId(), updateRequest);

        assertEquals(5, updated.getQuantity());
        assertEquals(299.99, updated.getTotalPrice());
        assertEquals("Updated Address", updated.getShippingAddress());
        assertEquals(created.getId(), updated.getId());
    }

    @Test
    void testUpdateOrderStatus_Success() {
        OrderResponse created = orderService.createOrder(validOrderRequest);

        OrderResponse updated = orderService.updateOrderStatus(created.getId(), OrderStatus.PROCESSING);

        assertEquals(OrderStatus.PROCESSING, updated.getStatus());
        assertEquals(created.getId(), updated.getId());
    }

    @Test
    void testDeleteOrder_Success() {
        OrderResponse created = orderService.createOrder(validOrderRequest);

        orderService.deleteOrder(created.getId());

        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(created.getId());
        });
    }

    @Test
    void testDeleteOrder_NotFound() {
        assertThrows(RuntimeException.class, () -> {
            orderService.deleteOrder(99999L);
        });
    }

    @Test
    void testOrderStatusTransitions() {
        OrderResponse order = orderService.createOrder(validOrderRequest);

        // Test status transitions
        order = orderService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        order = orderService.updateOrderStatus(order.getId(), OrderStatus.SENT);
        assertEquals(OrderStatus.SENT, order.getStatus());

        order = orderService.updateOrderStatus(order.getId(), OrderStatus.ACCEPTED);
        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
    }
} 
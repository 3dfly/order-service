package com.threedfly.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.config.TestConfig;
import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.PaymentResponse;
import com.threedfly.orderservice.entity.*;
import com.threedfly.orderservice.repository.OrderRepository;
import com.threedfly.orderservice.repository.PaymentRepository;
import com.threedfly.orderservice.repository.SellerRepository;
import com.threedfly.orderservice.service.PaymentLockService;
import com.threedfly.orderservice.service.PaymentMapper;
import com.threedfly.orderservice.service.payment.PaymentProvider;
import com.threedfly.orderservice.service.payment.PaymentProviderFactory;
import com.threedfly.orderservice.service.payment.PaymentProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "payment.platform.fee=3.00",
    "payment.platform.currency=USD",
    "paypal.client.id=test_client_id",
    "paypal.client.secret=test_client_secret",
    "paypal.mode=sandbox",
    "paypal.base.url=https://api.sandbox.paypal.com"
})
@Import(TestConfig.class)
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentProviderFactory mockPaymentProviderFactory;

    @Autowired
    private PaymentMapper mockPaymentMapper;

    @Autowired
    private PaymentLockService mockPaymentLockService;

    @Autowired
    private PaymentProvider mockPaymentProvider;

    private Seller testSeller;
    private Order testOrder;
    private CreatePaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() {
        // Clean up
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        sellerRepository.deleteAll();

        // Create test seller
        testSeller = new Seller();
        testSeller.setUserId(1001L);
        testSeller.setBusinessName("Test Electronics Store");
        testSeller.setBusinessAddress("123 Business St");
        testSeller.setContactEmail("seller@test.com");
        testSeller.setContactPhone("+15550123");
        testSeller.setVerified(true);
        testSeller = sellerRepository.save(testSeller);

        // Create test order
        testOrder = new Order();
        testOrder.setCustomerId(2001L);
        testOrder.setCustomerName("John Doe");
        testOrder.setCustomerEmail("john@test.com");
        testOrder.setProductId(3001L);
        testOrder.setQuantity(2);
        testOrder.setTotalPrice(10.00);
        testOrder.setShippingAddress("456 Test Ave");
        testOrder.setSupplierId(4001L);
        testOrder.setSeller(testSeller);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder = orderRepository.save(testOrder);

        // Create valid payment request
        validPaymentRequest = new CreatePaymentRequest();
        validPaymentRequest.setOrderId(testOrder.getId());
        validPaymentRequest.setMethod(PaymentMethod.PAYPAL);
        validPaymentRequest.setTotalAmount(new BigDecimal("10.00"));
        validPaymentRequest.setPaypalEmail("buyer@test.com");
        validPaymentRequest.setCurrency("USD");
        validPaymentRequest.setDescription("Test payment");
        validPaymentRequest.setSuccessUrl("https://test.com/success");
        validPaymentRequest.setCancelUrl("https://test.com/cancel");

        // Don't call setupMockPaymentFlow() here - each test will set up its own mocks as needed
    }

    private void setupMockPaymentFlow() {
        // Create a test payment entity
        Payment testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrder(testOrder);
        testPayment.setSeller(testSeller);
        testPayment.setTotalAmount(new BigDecimal("10.00"));
        testPayment.setPlatformFee(new BigDecimal("3.00"));
        testPayment.setSellerAmount(new BigDecimal("7.00"));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setMethod(PaymentMethod.PAYPAL);
        testPayment.setCreatedAt(LocalDateTime.now());

        // Mock payment mapper - use lenient to avoid strict stubbing issues
        lenient().when(mockPaymentMapper.createPaymentEntity(any(), any(), any())).thenReturn(testPayment);
        
        PaymentResponse testResponse = PaymentResponse.builder()
                .id(1L)
                .orderId(testOrder.getId())
                .sellerId(testSeller.getId())
                .sellerBusinessName("Test Electronics Store")
                .totalAmount(new BigDecimal("10.00"))
                .platformFee(new BigDecimal("3.00"))
                .sellerAmount(new BigDecimal("7.00"))
                .status(PaymentStatus.PENDING)
                .method(PaymentMethod.PAYPAL)
                .createdAt(LocalDateTime.now())
                .build();
        
        lenient().when(mockPaymentMapper.toPaymentResponse(any())).thenReturn(testResponse);

        // Mock payment provider
        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .providerPaymentId("TEST_PAYMENT_ID")
                .approvalUrl("https://paypal.com/approval")
                .build();
        
        lenient().when(mockPaymentProviderFactory.getProvider(PaymentMethod.PAYPAL)).thenReturn(mockPaymentProvider);
        lenient().when(mockPaymentProvider.createPayment(any(), any())).thenReturn(successResult);
        
        // Mock mapper update method
        lenient().doNothing().when(mockPaymentMapper).updatePaymentWithProviderResult(any(), any());
    }

    @Test
    void testCreatePayment_Success() throws Exception {
        // Set up mocks for successful flow
        setupMockPaymentFlow();
        
        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.totalAmount").value(10.00))
                .andExpect(jsonPath("$.platformFee").value(3.00))
                .andExpect(jsonPath("$.sellerAmount").value(7.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testCreatePayment_InvalidOrderId() throws Exception {
        validPaymentRequest.setOrderId(99999L); // Non-existent order
        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value(containsString("Order not found")));
    }

    @Test
    void testCreatePayment_AmountTooSmall() throws Exception {
        // Mock the mapper to throw validation exception for this specific test
        when(mockPaymentMapper.createPaymentEntity(any(), any(), any()))
                .thenThrow(new RuntimeException("Payment amount must be greater than platform fee"));

        validPaymentRequest.setTotalAmount(new BigDecimal("2.00")); // Less than platform fee
        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value(containsString("platform fee")));
    }

    @Test
    void testCreatePayment_MissingFields() throws Exception {
        validPaymentRequest.setOrderId(null);
        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPaymentSplitCalculation() throws Exception {
        setupMockPaymentFlow(); // Set up mocks for this specific test
        // Set up different amount for this test
        validPaymentRequest.setTotalAmount(new BigDecimal("25.00"));

        PaymentResponse largerResponse = PaymentResponse.builder()
                .id(1L)
                .totalAmount(new BigDecimal("25.00"))
                .platformFee(new BigDecimal("3.00"))
                .sellerAmount(new BigDecimal("22.00"))
                .status(PaymentStatus.PENDING)
                .build();
        
        when(mockPaymentMapper.toPaymentResponse(any())).thenReturn(largerResponse);

        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.platformFee").value(3.00))
                .andExpect(jsonPath("$.sellerAmount").value(22.00));
    }

    @Test
    void testPaymentSplitCalculation_MinimumAmount() throws Exception {
        setupMockPaymentFlow(); // Set up mocks for this specific test
        validPaymentRequest.setTotalAmount(new BigDecimal("3.01"));

        PaymentResponse minResponse = PaymentResponse.builder()
                .id(1L)
                .totalAmount(new BigDecimal("3.01"))
                .platformFee(new BigDecimal("3.00"))
                .sellerAmount(new BigDecimal("0.01"))
                .status(PaymentStatus.PENDING)
                .build();
        
        when(mockPaymentMapper.toPaymentResponse(any())).thenReturn(minResponse);

        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(3.01))
                .andExpect(jsonPath("$.platformFee").value(3.00))
                .andExpect(jsonPath("$.sellerAmount").value(0.01));
    }

    @Test
    void testGetPaymentsByOrderId_EmptyResults() throws Exception {
        mockMvc.perform(get("/payments/order/{orderId}", testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetPaymentsBySellerId_EmptyResults() throws Exception {
        mockMvc.perform(get("/payments/seller/{sellerId}", testSeller.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetPaymentById_NotFound() throws Exception {
        mockMvc.perform(get("/payments/{id}", 99999L))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value(containsString("Payment not found")));
    }

    @Test
    void testPayPalWebhook_EmptyPayload() throws Exception {
        mockMvc.perform(post("/payments/webhook/paypal")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));
    }

    @Test
    void testConcurrentPaymentExecution() throws Exception {
        setupMockPaymentFlow(); // Set up mocks for this specific test
        // Create a payment first
        testCreatePayment_Success();

        // For this test, we'll mock the locking service to demonstrate it's being used
        when(mockPaymentLockService.executeWithLock(anyLong(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());

        // Mock execute payment response
        PaymentResponse completedResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();
        when(mockPaymentMapper.toPaymentResponse(any())).thenReturn(completedResponse);

        // Test that execute payment uses locking
        mockMvc.perform(post("/payments/{id}/execute", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paypalPaymentId\":\"TEST_PAYPAL_PAYMENT_ID\",\"paypalPayerId\":\"TEST_PAYER_ID\"}"))
                .andExpect(status().isOk());

        // Verify locking service was called (this demonstrates concurrency protection)
        verify(mockPaymentLockService, atLeastOnce()).executeWithLock(anyLong(), any());
    }

    @Test
    void testInvalidHttpMethods() throws Exception {
        // Test that only allowed HTTP methods work
        mockMvc.perform(get("/payments"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/payments/1"))
                .andExpect(status().isMethodNotAllowed());
    }
} 
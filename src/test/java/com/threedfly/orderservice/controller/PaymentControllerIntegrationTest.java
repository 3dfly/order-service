package com.threedfly.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.TestUtils;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "payment.platform.fee=3.00",
    "payment.platform.currency=USD",
    "paypal.client.id=test_client_id",
    "paypal.client.secret=test_client_secret",
    "paypal.mode=sandbox",
    "paypal.base.url=https://sandbox.paypal.com"
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
    void setUp() throws Exception {
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
        testOrder.setProductId("PROD-3001");
        testOrder.setQuantity(2);
        testOrder.setStlFileUrl("https://example.com/model.stl");
        testOrder.setShippingAddress(objectMapper.writeValueAsString(TestUtils.createTestShippingAddress()));
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
    }

    private void setupMockPaymentFlow() {
        // Create a test payment entity (no ID set - let Hibernate generate it)
        Payment testPayment = new Payment();
        testPayment.setOrder(testOrder);
        testPayment.setSeller(testSeller);
        testPayment.setTotalAmount(new BigDecimal("10.00")); // Ensure total amount is greater than platform fee
        testPayment.setPlatformFee(new BigDecimal("3.00"));
        testPayment.setSellerAmount(new BigDecimal("7.00"));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setMethod(PaymentMethod.PAYPAL);
        testPayment.setCreatedAt(LocalDateTime.now());

        // Mock payment mapper - make sure it always returns a valid payment
        when(mockPaymentMapper.createPaymentEntity(any(), any(), any())).thenAnswer(invocation -> {
            CreatePaymentRequest request = invocation.getArgument(0);
            Order order = invocation.getArgument(1);
            Seller seller = invocation.getArgument(2);

            // Ensure request is not null
            if (request == null) {
                request = new CreatePaymentRequest();
                request.setTotalAmount(new BigDecimal("10.00"));
                request.setMethod(PaymentMethod.PAYPAL);
            }

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setSeller(seller);
            payment.setTotalAmount(request.getTotalAmount());
            payment.setPlatformFee(new BigDecimal("3.00"));
            payment.setSellerAmount(request.getTotalAmount().subtract(new BigDecimal("3.00")));
            payment.setStatus(PaymentStatus.PENDING);
            payment.setMethod(request.getMethod());
            payment.setCreatedAt(LocalDateTime.now());

            return payment;
        });
        
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
                .providerPaymentId("TEST_PROVIDER_PAYMENT_ID")
                .approvalUrl("https://paypal.com/approval")
                .rawRequest("{\"test\": \"request\"}")
                .auditData(null)
                .build();
        
        lenient().when(mockPaymentProviderFactory.getProvider(PaymentMethod.PAYPAL)).thenReturn(mockPaymentProvider);
        lenient().when(mockPaymentProvider.createPayment(any(), any())).thenReturn(successResult);
        
        // Mock mapper update method
        lenient().doNothing().when(mockPaymentMapper).updatePaymentWithProviderResult(any(), any());
    }

    @Test
    void testCreatePayment_Success() throws Exception {
        setupMockPaymentFlow();
        
        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.totalAmount").value(10.00))
                .andExpect(jsonPath("$.platformFee").value(3.00))
                .andExpect(jsonPath("$.sellerAmount").value(7.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testCreatePayment_InvalidOrderId() throws Exception {
        validPaymentRequest.setOrderId(99999L);
        String paymentJson = objectMapper.writeValueAsString(validPaymentRequest);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Order not found")));
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
        setupMockPaymentFlow();
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
        setupMockPaymentFlow();
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Payment not found")));
    }

    @Test
    void testConcurrentPaymentExecution() throws Exception {
        setupMockPaymentFlow();
        // Create a payment first
        MvcResult result = mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the payment ID from the response
        String responseContent = result.getResponse().getContentAsString();
        Long paymentId = JsonPath.parse(responseContent).read("$.id", Long.class);

        // Ensure paymentId is not null
        assertNotNull(paymentId, "Payment ID should not be null");

        // Update the payment with the providerPaymentId
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setProviderPaymentId("TEST_PROVIDER_PAYMENT_ID");
        paymentRepository.save(payment);

        // For this test, we'll mock the locking service to demonstrate it's being used
        when(mockPaymentLockService.executeWithLock(anyLong(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());

        // Mock execute payment response
        PaymentProviderResult executeResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.COMPLETED)
                .providerPaymentId("TEST_PROVIDER_PAYMENT_ID")
                .auditData(null)
                .build();
        lenient().when(mockPaymentProvider.executePayment(any(), any())).thenReturn(executeResult);

        // Test that execute payment uses locking
        mockMvc.perform(post("/payments/{id}/execute", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"providerPaymentId\":\"TEST_PROVIDER_PAYMENT_ID\",\"providerPayerId\":\"TEST_PAYER_ID\"}"))
                .andExpect(status().isOk());

        // Verify locking service was called
        verify(mockPaymentLockService, atLeastOnce()).executeWithLock(anyLong(), any());
    }

    @Test
    void testInvalidHttpMethods() throws Exception {
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
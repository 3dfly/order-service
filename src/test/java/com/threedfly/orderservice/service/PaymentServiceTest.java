package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.ExecutePaymentRequest;
import com.threedfly.orderservice.dto.PaymentResponse;
import com.threedfly.orderservice.entity.*;
import com.threedfly.orderservice.repository.OrderRepository;
import com.threedfly.orderservice.repository.PaymentRepository;
import com.threedfly.orderservice.repository.SellerRepository;
import com.threedfly.orderservice.service.payment.PaymentProvider;
import com.threedfly.orderservice.service.payment.PaymentProviderFactory;
import com.threedfly.orderservice.service.payment.PaymentProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PaymentProviderFactory paymentProviderFactory;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentLockService paymentLockService;

    @Mock
    private PaymentAuditService paymentAuditService;

    @Mock
    private PaymentProvider mockPaymentProvider;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private Seller testSeller;
    private Payment testPayment;
    private CreatePaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() {
        // Setup test seller
        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setBusinessName("Test Electronics Store");
        testSeller.setContactEmail("seller@test.com");
        testSeller.setVerified(true);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setSeller(testSeller);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalPrice(10.00);

        // Setup test payment
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrder(testOrder);
        testPayment.setSeller(testSeller);
        testPayment.setTotalAmount(new BigDecimal("10.00"));
        testPayment.setPlatformFee(new BigDecimal("3.00"));
        testPayment.setSellerAmount(new BigDecimal("7.00"));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setMethod(PaymentMethod.PAYPAL);
        testPayment.setCreatedAt(LocalDateTime.now());

        // Setup valid payment request
        validPaymentRequest = new CreatePaymentRequest();
        validPaymentRequest.setOrderId(1L);
        validPaymentRequest.setMethod(PaymentMethod.PAYPAL);
        validPaymentRequest.setTotalAmount(new BigDecimal("10.00"));
        validPaymentRequest.setPaypalEmail("buyer@test.com");
        validPaymentRequest.setCurrency("USD");
        validPaymentRequest.setDescription("Test payment");
        validPaymentRequest.setSuccessUrl("https://test.com/success");
        validPaymentRequest.setCancelUrl("https://test.com/cancel");
    }

    @Test
    void testCreatePayment_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentMapper.createPaymentEntity(any(), any(), any())).thenReturn(testPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentProviderFactory.getProvider(any(PaymentMethod.class))).thenReturn(mockPaymentProvider);

        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .providerPaymentId("TEST_PAYMENT_ID")
                .approvalUrl("https://paypal.com/approval")
                .build();

        when(mockPaymentProvider.createPayment(any(), any())).thenReturn(successResult);
        doNothing().when(paymentMapper).updatePaymentWithProviderResult(any(), any());

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id(1L)
                .orderId(1L)
                .sellerId(1L)
                .totalAmount(new BigDecimal("10.00"))
                .platformFee(new BigDecimal("3.00"))
                .sellerAmount(new BigDecimal("7.00"))
                .status(PaymentStatus.PENDING)
                .method(PaymentMethod.PAYPAL)
                .build();

        when(paymentMapper.toPaymentResponse(any())).thenReturn(expectedResponse);

        // Act
        PaymentResponse result = paymentService.createPayment(validPaymentRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentProviderFactory).getProvider(any(PaymentMethod.class));
        verify(mockPaymentProvider).createPayment(testPayment, validPaymentRequest);
        verify(paymentMapper).updatePaymentWithProviderResult(testPayment, successResult);
    }

    @Test
    void testCreatePayment_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        validPaymentRequest.setOrderId(999L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createPayment(validPaymentRequest);
        });

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(paymentProviderFactory, never()).getProvider(any(PaymentMethod.class));
    }

    @Test
    void testCreatePayment_NoSeller() {
        testOrder.setSeller(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            paymentService.createPayment(validPaymentRequest));

        assertTrue(exception.getMessage().contains("associated seller"));
        verify(orderRepository).findById(1L);
    }

    @Test
    void testExecutePayment_Success() {
        // Arrange
        testPayment.setProviderPaymentId("TEST_PROVIDER_ID");
        testPayment.setStatus(PaymentStatus.PENDING);

        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("TEST_PROVIDER_ID");
        executeRequest.setProviderPayerId("TEST_PAYER_ID");

        when(paymentRepository.findByProviderPaymentId("TEST_PROVIDER_ID")).thenReturn(Optional.of(testPayment));
        when(paymentProviderFactory.getProvider(any(PaymentMethod.class))).thenReturn(mockPaymentProvider);

        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.COMPLETED)
                .platformTransactionId("platform_txn_123")
                .build();

        when(mockPaymentProvider.executePayment(any(), any())).thenReturn(successResult);

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentMapper.toPaymentResponse(any())).thenReturn(expectedResponse);

        // Mock the lock service to execute the lambda
        when(paymentLockService.executeWithLock(eq(1L), any())).thenAnswer(invocation -> {
            return invocation.getArgument(1, java.util.function.Supplier.class).get();
        });

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        // Act
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
    }

    @Test
    void testExecutePayment_PaymentNotFound() {
        // Arrange
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("NON_EXISTENT_ID");

        when(paymentRepository.findByProviderPaymentId("NON_EXISTENT_ID")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.executePayment(executeRequest);
        });

        assertTrue(exception.getMessage().contains("Payment not found"));
    }

    @Test
    void testExecutePayment_AlreadyCompleted() {
        // Arrange
        testPayment.setProviderPaymentId("TEST_PROVIDER_ID");
        testPayment.setStatus(PaymentStatus.COMPLETED);

        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("TEST_PROVIDER_ID");

        when(paymentRepository.findByProviderPaymentId("TEST_PROVIDER_ID")).thenReturn(Optional.of(testPayment));

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentMapper.toPaymentResponse(any())).thenReturn(expectedResponse);

        // Mock the lock service to execute the lambda (lenient because of preliminary check optimization)
        lenient().when(paymentLockService.executeWithLock(eq(1L), any())).thenAnswer(invocation -> {
            return invocation.getArgument(1, java.util.function.Supplier.class).get();
        });

        lenient().when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        // Act
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Verify it returns the existing completed payment without processing
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentProviderFactory, never()).getProvider(any(PaymentMethod.class));
        verify(mockPaymentProvider, never()).executePayment(any(), any());
    }

    @Test
    void testGetPaymentById_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        PaymentResponse mockResponse = PaymentResponse.builder().id(1L).build();
        when(paymentMapper.toPaymentResponse(testPayment)).thenReturn(mockResponse);

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(paymentRepository).findById(1L);
    }

    @Test
    void testGetPaymentById_NotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            paymentService.getPaymentById(1L));

        assertTrue(exception.getMessage().contains("Payment not found"));
    }

    @Test
    void testGetPaymentsByOrderId() {
        // Arrange
        List<Payment> payments = List.of(testPayment);
        when(paymentRepository.findByOrderId(1L)).thenReturn(payments);
        when(paymentMapper.toPaymentResponse(testPayment)).thenReturn(PaymentResponse.builder().id(1L).build());

        // Act
        List<PaymentResponse> result = paymentService.getPaymentsByOrderId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPaymentsBySellerId() {
        // Arrange
        List<Payment> payments = List.of(testPayment);
        when(paymentRepository.findBySellerId(1L)).thenReturn(payments);
        when(paymentMapper.toPaymentResponse(testPayment)).thenReturn(PaymentResponse.builder().id(1L).build());

        // Act
        List<PaymentResponse> result = paymentService.getPaymentsBySellerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPlatformFeesInDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        when(paymentRepository.getTotalPlatformFeesForPeriod(start, end)).thenReturn(150.0);

        BigDecimal result = paymentService.getPlatformFeesInDateRange(start, end);

        assertEquals(0, new BigDecimal("150.0").compareTo(result));
        verify(paymentRepository).getTotalPlatformFeesForPeriod(start, end);
    }

    @Test
    void testGetSellerEarningsInDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        when(paymentRepository.getTotalSellerEarningsForPeriod(1L, start, end)).thenReturn(500.0);

        BigDecimal result = paymentService.getSellerEarningsInDateRange(1L, start, end);

        assertEquals(0, new BigDecimal("500.0").compareTo(result));
        verify(paymentRepository).getTotalSellerEarningsForPeriod(1L, start, end);
    }

    @Test
    void testConcurrentPaymentExecution() throws Exception {
        // Arrange
        testPayment.setProviderPaymentId("TEST_CONCURRENT_ID");
        testPayment.setStatus(PaymentStatus.PENDING);

        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("TEST_CONCURRENT_ID");
        executeRequest.setProviderPayerId("TEST_PAYER_ID");

        when(paymentRepository.findByProviderPaymentId("TEST_CONCURRENT_ID")).thenReturn(Optional.of(testPayment));

        // Mock lock service behavior - this simulates the locking mechanism
        when(paymentLockService.executeWithLock(eq(1L), any())).thenAnswer(invocation -> {
            // Simulate that the lock was acquired and the payment was processed
            return invocation.getArgument(1, java.util.function.Supplier.class).get();
        });

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);
        when(paymentProviderFactory.getProvider(any(PaymentMethod.class))).thenReturn(mockPaymentProvider);

        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(mockPaymentProvider.executePayment(any(), any())).thenReturn(successResult);

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentMapper.toPaymentResponse(any())).thenReturn(expectedResponse);

        // Act
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentLockService).executeWithLock(eq(1L), any());
    }

    @Test
    void testHandlePaymentWebhook() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> paymentService.handlePaymentWebhook("{}", "PayPal"));
    }
} 
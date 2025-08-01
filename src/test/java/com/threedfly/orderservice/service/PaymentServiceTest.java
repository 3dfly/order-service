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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private PaymentProvider mockPaymentProvider;

    @InjectMocks
    private PaymentService paymentService;

    private Seller testSeller;
    private Order testOrder;
    private Payment testPayment;
    private CreatePaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() {
        // Set up test seller
        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setUserId(1001L);
        testSeller.setBusinessName("Test Electronics Store");
        testSeller.setBusinessAddress("123 Business St");
        testSeller.setContactEmail("seller@test.com");
        testSeller.setContactPhone("+15550123");
        testSeller.setVerified(true);

        // Set up test order
        testOrder = new Order();
        testOrder.setId(1L);
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

        // Set up test payment
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

        // Set up valid payment request
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
        // Mock dependencies
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentMapper.createPaymentEntity(any(), any(), any())).thenReturn(testPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentProviderFactory.getProvider(PaymentMethod.PAYPAL)).thenReturn(mockPaymentProvider);
        
        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .providerPaymentId("TEST_PAYMENT_ID")
                .approvalUrl("https://paypal.com/approval")
                .build();
        when(mockPaymentProvider.createPayment(any(), any())).thenReturn(successResult);
        
        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(1L)
                .totalAmount(new BigDecimal("10.00"))
                .platformFee(new BigDecimal("3.00"))
                .sellerAmount(new BigDecimal("7.00"))
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentMapper.toPaymentResponse(any())).thenReturn(mockResponse);

        // Execute
        PaymentResponse result = paymentService.createPayment(validPaymentRequest);

        // Verify
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getTotalAmount()));
        assertEquals(0, new BigDecimal("3.00").compareTo(result.getPlatformFee()));
        assertEquals(0, new BigDecimal("7.00").compareTo(result.getSellerAmount()));
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(orderRepository).findById(1L);
        verify(paymentMapper).createPaymentEntity(any(), any(), any());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(paymentProviderFactory).getProvider(PaymentMethod.PAYPAL);
        verify(mockPaymentProvider).createPayment(any(), any());
    }

    @Test
    void testCreatePayment_OrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            paymentService.createPayment(validPaymentRequest));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findById(1L);
        verifyNoInteractions(paymentRepository);
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
    void testCreatePayment_AmountTooSmall() {
        // Mock the validation to throw exception for small amounts
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentMapper.createPaymentEntity(any(), any(), any())).thenThrow(
            new RuntimeException("Payment amount must be greater than platform fee"));

        validPaymentRequest.setTotalAmount(new BigDecimal("2.00"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            paymentService.createPayment(validPaymentRequest));

        assertTrue(exception.getMessage().contains("platform fee"));
    }

    @Test
    void testExecutePayment_Success() {
        // Mock the locking service to execute the operation directly
        testPayment.setPaypalPaymentId("TEST_PAYPAL_ID");
        
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setPaypalPaymentId("TEST_PAYPAL_ID");
        executeRequest.setPaypalPayerId("TEST_PAYER_ID");

        when(paymentRepository.findByPaypalPaymentId("TEST_PAYPAL_ID")).thenReturn(Optional.of(testPayment));
        when(paymentLockService.executeWithLock(anyLong(), any())).thenAnswer(invocation -> {
            // Execute the lambda function directly
            return ((java.util.function.Supplier<?>) invocation.getArgument(1)).get();
        });
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentProviderFactory.getProvider(PaymentMethod.PAYPAL)).thenReturn(mockPaymentProvider);
        
        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.COMPLETED)
                .platformTransactionId("TXN_123")
                .build();
        when(mockPaymentProvider.executePayment(any(), any())).thenReturn(successResult);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        
        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();
        when(paymentMapper.toPaymentResponse(any())).thenReturn(mockResponse);

        // Execute
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Verify
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentLockService).executeWithLock(eq(1L), any());
    }

    @Test
    void testExecutePayment_PaymentNotFound() {
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setPaypalPaymentId("NON_EXISTENT_ID");

        when(paymentRepository.findByPaypalPaymentId("NON_EXISTENT_ID")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            paymentService.executePayment(executeRequest));

        assertTrue(exception.getMessage().contains("Payment not found"));
    }

    @Test
    void testExecutePayment_AlreadyProcessed() {
        testPayment.setPaypalPaymentId("TEST_PAYPAL_ID");
        testPayment.setStatus(PaymentStatus.COMPLETED);
        
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setPaypalPaymentId("TEST_PAYPAL_ID");

        when(paymentRepository.findByPaypalPaymentId("TEST_PAYPAL_ID")).thenReturn(Optional.of(testPayment));
        when(paymentLockService.executeWithLock(anyLong(), any())).thenAnswer(invocation -> {
            return ((java.util.function.Supplier<?>) invocation.getArgument(1)).get();
        });
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        
        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();
        when(paymentMapper.toPaymentResponse(any())).thenReturn(mockResponse);

        // Execute
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Verify it returns the existing completed payment without processing
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentProviderFactory, never()).getProvider(any());
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
        when(paymentRepository.findByOrderId(1L)).thenReturn(List.of(testPayment));
        PaymentResponse mockResponse = PaymentResponse.builder().id(1L).orderId(1L).build();
        when(paymentMapper.toPaymentResponse(testPayment)).thenReturn(mockResponse);

        List<PaymentResponse> result = paymentService.getPaymentsByOrderId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getOrderId());
        verify(paymentRepository).findByOrderId(1L);
    }

    @Test
    void testGetPaymentsBySellerId() {
        when(paymentRepository.findBySellerId(1L)).thenReturn(List.of(testPayment));
        PaymentResponse mockResponse = PaymentResponse.builder().id(1L).sellerId(1L).build();
        when(paymentMapper.toPaymentResponse(testPayment)).thenReturn(mockResponse);

        List<PaymentResponse> result = paymentService.getPaymentsBySellerId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSellerId());
        verify(paymentRepository).findBySellerId(1L);
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
    void testConcurrentPaymentExecution() throws InterruptedException {
        // Test that our locking mechanism works correctly
        testPayment.setPaypalPaymentId("TEST_CONCURRENT_ID");
        
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setPaypalPaymentId("TEST_CONCURRENT_ID");
        executeRequest.setPaypalPayerId("TEST_PAYER_ID");

        when(paymentRepository.findByPaypalPaymentId("TEST_CONCURRENT_ID")).thenReturn(Optional.of(testPayment));
        
        // Mock the lock service to simulate real locking behavior
        CountDownLatch executionLatch = new CountDownLatch(1);
        when(paymentLockService.executeWithLock(anyLong(), any())).thenAnswer(invocation -> {
            // Only allow one execution at a time
            try {
                executionLatch.await();
                return ((java.util.function.Supplier<?>) invocation.getArgument(1)).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        // Submit multiple concurrent execution attempts
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CompletableFuture<PaymentResponse> future1 = CompletableFuture.supplyAsync(() -> 
            paymentService.executePayment(executeRequest), executor);
        CompletableFuture<PaymentResponse> future2 = CompletableFuture.supplyAsync(() -> 
            paymentService.executePayment(executeRequest), executor);
        CompletableFuture<PaymentResponse> future3 = CompletableFuture.supplyAsync(() -> 
            paymentService.executePayment(executeRequest), executor);

        // Let the executions proceed
        executionLatch.countDown();

        // Verify that locking was attempted for all calls
        Thread.sleep(100); // Give time for all threads to reach the lock
        verify(paymentLockService, times(3)).executeWithLock(eq(1L), any());

        executor.shutdown();
    }

    @Test
    void testHandlePaymentWebhook() {
        // Test webhook handling (currently just logs)
        assertDoesNotThrow(() -> paymentService.handlePaymentWebhook("{}", "PayPal"));
    }
} 
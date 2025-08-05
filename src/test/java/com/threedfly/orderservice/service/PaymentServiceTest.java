package com.threedfly.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.TestUtils;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    void setUp() throws Exception {
        // Create test seller
        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setUserId(1001L);
        testSeller.setBusinessName("Test Store");
        testSeller.setBusinessAddress("123 Test St");
        testSeller.setContactEmail("test@store.com");
        testSeller.setContactPhone("+15550123");
        testSeller.setVerified(true);

        // Create test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setCustomerId(2001L);
        testOrder.setProductId("PROD-1001");
        testOrder.setQuantity(2);
        testOrder.setStlFileUrl("https://example.com/model.stl");
        testOrder.setShippingAddress(new ObjectMapper().writeValueAsString(TestUtils.createTestShippingAddress()));
        testOrder.setSupplierId(3001L);
        testOrder.setSeller(testSeller);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setOrderDate(LocalDateTime.now());

        // Create test payment
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
        when(paymentProviderFactory.getProvider(any(PaymentMethod.class))).thenReturn(mockPaymentProvider);

        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.PENDING)
                .providerPaymentId("TEST_PAYMENT_ID")
                .approvalUrl("https://test.com/approve")
                .build();

        PaymentProviderResult supplierPayoutResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.COMPLETED)
                .sellerTransactionId("TEST_PAYOUT_ID")
                .build();

        when(mockPaymentProvider.createPayment(any(), any())).thenReturn(successResult);
        when(mockPaymentProvider.payoutToSupplier(any(), any())).thenReturn(supplierPayoutResult);
        when(paymentRepository.save(any())).thenReturn(testPayment);

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id(1L)
                .orderId(1L)
                .sellerId(1L)
                .sellerBusinessName("Test Store")
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
        verify(paymentProviderFactory, times(2)).getProvider(any(PaymentMethod.class)); // Called twice: once for initial payment, once for supplier payout
        verify(mockPaymentProvider).createPayment(testPayment, validPaymentRequest);
        verify(mockPaymentProvider).payoutToSupplier(testPayment, "supplier3001@example.com");
        verify(paymentMapper).updatePaymentWithProviderResult(testPayment, successResult);
    }

    @Test
    void testCreatePayment_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> paymentService.createPayment(validPaymentRequest));
    }

    @Test
    void testCreatePayment_NoSeller() {
        // Arrange
        testOrder.setSeller(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> paymentService.createPayment(validPaymentRequest));
    }

    @Test
    void testExecutePayment_Success() {
        // Arrange
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("TEST_PAYMENT_ID");
        executeRequest.setProviderPayerId("TEST_PAYER_ID");

        when(paymentRepository.findByProviderPaymentId("TEST_PAYMENT_ID")).thenReturn(Optional.of(testPayment));
        when(paymentProviderFactory.getProvider(PaymentMethod.PAYPAL)).thenReturn(mockPaymentProvider);

        PaymentProviderResult successResult = PaymentProviderResult.builder()
                .success(true)
                .status(PaymentStatus.COMPLETED)
                .providerPaymentId("TEST_PAYMENT_ID")
                .build();

        when(mockPaymentProvider.executePayment(any(), any())).thenReturn(successResult);
        when(paymentRepository.save(any())).thenReturn(testPayment);
        when(paymentRepository.findById(any())).thenReturn(Optional.of(testPayment));

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentMapper.toPaymentResponse(any())).thenReturn(expectedResponse);
        when(paymentLockService.executeWithLock(anyLong(), any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(1)).get());

        // Act
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentLockService).executeWithLock(anyLong(), any());
    }

    @Test
    void testExecutePayment_PaymentNotFound() {
        // Arrange
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("NONEXISTENT_ID");

        when(paymentRepository.findByProviderPaymentId("NONEXISTENT_ID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> paymentService.executePayment(executeRequest));
    }

    @Test
    void testExecutePayment_AlreadyCompleted() {
        // Arrange
        ExecutePaymentRequest executeRequest = new ExecutePaymentRequest();
        executeRequest.setProviderPaymentId("TEST_PAYMENT_ID");

        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findByProviderPaymentId("TEST_PAYMENT_ID")).thenReturn(Optional.of(testPayment));
        when(paymentMapper.toPaymentResponse(any())).thenReturn(PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.COMPLETED)
                .build());

        // Act
        PaymentResponse result = paymentService.executePayment(executeRequest);

        // Assert
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(mockPaymentProvider, never()).executePayment(any(), any());
    }
}
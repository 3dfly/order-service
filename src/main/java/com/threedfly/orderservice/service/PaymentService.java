package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.CreatePaymentRequest;
import com.threedfly.orderservice.dto.ExecutePaymentRequest;
import com.threedfly.orderservice.dto.PaymentResponse;
import com.threedfly.orderservice.entity.Order;
import com.threedfly.orderservice.entity.Payment;
import com.threedfly.orderservice.entity.PaymentStatus;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.repository.OrderRepository;
import com.threedfly.orderservice.repository.PaymentRepository;
import com.threedfly.orderservice.repository.SellerRepository;
import com.threedfly.orderservice.service.payment.PaymentProvider;
import com.threedfly.orderservice.service.payment.PaymentProviderFactory;
import com.threedfly.orderservice.service.payment.PaymentProviderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thread-safe PaymentService with proper architecture:
 * 1. Uses factory pattern for payment providers
 * 2. Uses mappers instead of manual field setting
 * 3. Implements concurrency protection with locking
 * 4. Proper DTOs instead of Map objects
 * 5. Extensible for multiple payment methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final PaymentMapper paymentMapper;
    private final PaymentLockService paymentLockService;

    /**
     * Create a new payment using factory pattern for provider selection
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("💳 Creating payment for order: {} with method: {}", request.getOrderId(), request.getMethod());

        try {
            // Validate order exists and get seller
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + request.getOrderId()));

            Seller seller = order.getSeller();
            if (seller == null) {
                throw new RuntimeException("Order must have an associated seller for payment processing");
            }

            // Create payment entity using mapper (cleaner than manual field setting)
            Payment payment = paymentMapper.createPaymentEntity(request, order, seller);

            // Save payment first to get ID
            payment = paymentRepository.save(payment);

            log.info("💰 Payment calculation - Total: ${}, Platform: ${}, Seller: ${}", 
                    payment.getTotalAmount(), payment.getPlatformFee(), payment.getSellerAmount());

            // Get appropriate payment provider using factory pattern
            PaymentProvider provider = paymentProviderFactory.getProvider(request.getMethod());
            
            // Process payment with provider (uses proper DTOs, not Map objects)
            PaymentProviderResult result = provider.createPayment(payment, request);
            
            // Update payment with provider result using mapper
            paymentMapper.updatePaymentWithProviderResult(payment, result);

            // Save updated payment
            payment = paymentRepository.save(payment);

            if (result.isSuccess()) {
                log.info("✅ Payment created successfully with ID: {}", payment.getId());
            } else {
                log.error("❌ Payment creation failed: {}", result.getErrorMessage());
            }

            return paymentMapper.toPaymentResponse(payment);

        } catch (Exception e) {
            log.error("❌ Failed to create payment for order: {}", request.getOrderId(), e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    /**
     * Execute payment with CONCURRENCY PROTECTION to prevent duplicate execution
     */
    @Transactional
    public PaymentResponse executePayment(ExecutePaymentRequest request) {
        log.info("✅ Executing payment: {}", request.getPaypalPaymentId());

        // Find payment by provider payment ID
        Payment payment = paymentRepository.findByPaypalPaymentId(request.getPaypalPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found with PayPal ID: " + request.getPaypalPaymentId()));

        // **CRITICAL: Use locking mechanism to prevent concurrent execution**
        return paymentLockService.executeWithLock(payment.getId(), () -> {
            try {
                // Reload payment to get latest status (in case it was updated by another thread)
                Payment currentPayment = paymentRepository.findById(payment.getId())
                        .orElseThrow(() -> new RuntimeException("Payment not found"));

                // Check if payment is already processed
                if (currentPayment.getStatus() != PaymentStatus.PENDING) {
                    log.warn("⚠️ Payment {} is already in status: {} - skipping execution", 
                            currentPayment.getId(), currentPayment.getStatus());
                    return paymentMapper.toPaymentResponse(currentPayment);
                }

                // Mark as processing to prevent other threads
                currentPayment.setStatus(PaymentStatus.PROCESSING);
                currentPayment = paymentRepository.save(currentPayment);

                // Get appropriate payment provider using factory
                PaymentProvider provider = paymentProviderFactory.getProvider(currentPayment.getMethod());
                
                // Execute payment with provider
                PaymentProviderResult result = provider.executePayment(currentPayment, request);
                
                // Update payment with result using mapper
                paymentMapper.updatePaymentWithProviderResult(currentPayment, result);
                
                // Set provider-specific fields (could be moved to mapper)
                currentPayment.setPaypalPayerId(request.getPaypalPayerId());

                currentPayment = paymentRepository.save(currentPayment);
                
                log.info("✅ Payment {} executed successfully with status: {}", 
                        currentPayment.getId(), currentPayment.getStatus());
                return paymentMapper.toPaymentResponse(currentPayment);

            } catch (Exception e) {
                log.error("❌ Failed to execute payment: {}", request.getPaypalPaymentId(), e);
                
                // Update payment status to failed
                Payment failedPayment = paymentRepository.findById(payment.getId()).orElse(payment);
                failedPayment.setStatus(PaymentStatus.FAILED);
                failedPayment.setErrorMessage(e.getMessage());
                paymentRepository.save(failedPayment);
                
                throw new RuntimeException("Failed to execute payment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get payment by ID
     */
    public PaymentResponse getPaymentById(Long id) {
        log.info("🔍 Retrieving payment by ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));
        return paymentMapper.toPaymentResponse(payment);
    }

    /**
     * Get payments by order ID
     */
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        log.info("📋 Retrieving payments for order: {}", orderId);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    /**
     * Get payments by seller ID
     */
    public List<PaymentResponse> getPaymentsBySellerId(Long sellerId) {
        log.info("🏪 Retrieving payments for seller: {}", sellerId);
        List<Payment> payments = paymentRepository.findBySellerId(sellerId);
        return payments.stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    /**
     * Get platform fees collected in date range
     */
    public BigDecimal getPlatformFeesInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("💰 Calculating platform fees from {} to {}", startDate, endDate);
        Double result = paymentRepository.getTotalPlatformFeesForPeriod(startDate, endDate);
        return result != null ? BigDecimal.valueOf(result) : BigDecimal.ZERO;
    }

    /**
     * Get seller earnings in date range
     */
    public BigDecimal getSellerEarningsInDateRange(Long sellerId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("🏪 Calculating seller {} earnings from {} to {}", sellerId, startDate, endDate);
        Double result = paymentRepository.getTotalSellerEarningsForPeriod(sellerId, startDate, endDate);
        return result != null ? BigDecimal.valueOf(result) : BigDecimal.ZERO;
    }

    /**
     * Handle payment webhook (extensible for different providers)
     */
    public void handlePaymentWebhook(String payload, String providerName) {
        log.info("🔔 Received {} webhook", providerName);
        // Implementation would depend on the provider
        // This is where you'd update payment status based on webhook events
    }
} 
package com.threedfly.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Service providing concurrency control for payment operations
 * Prevents duplicate payment execution using per-payment locking
 */
@Service
@Slf4j
public class PaymentLockService {

    private final ConcurrentHashMap<Long, ReentrantLock> paymentLocks = new ConcurrentHashMap<>();

    /**
     * Execute a payment operation with exclusive locking to prevent concurrency issues
     * 
     * @param paymentId The payment ID to lock on
     * @param operation The operation to execute safely
     * @return The result of the operation
     */
    public <T> T executeWithLock(Long paymentId, Supplier<T> operation) {
        log.debug("🔒 Acquiring lock for payment: {}", paymentId);
        
        // Get or create a lock for this specific payment
        ReentrantLock lock = paymentLocks.computeIfAbsent(paymentId, k -> new ReentrantLock());
        
        lock.lock();
        try {
            log.debug("✅ Lock acquired for payment: {} by thread: {}", paymentId, Thread.currentThread().getName());
            
            // Execute the operation safely
            T result = operation.get();
            
            log.debug("🔓 Operation completed for payment: {}", paymentId);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error during locked operation for payment: {}", paymentId, e);
            throw e;
        } finally {
            lock.unlock();
            log.debug("🔓 Lock released for payment: {} by thread: {}", paymentId, Thread.currentThread().getName());
            
            // Clean up the lock if no other threads are waiting
            // This prevents memory leaks from accumulating locks
            if (!lock.hasQueuedThreads()) {
                paymentLocks.remove(paymentId, lock);
                log.debug("🧹 Cleaned up lock for payment: {}", paymentId);
            }
        }
    }

    /**
     * Check if a payment is currently locked (for testing/monitoring)
     */
    public boolean isPaymentLocked(Long paymentId) {
        ReentrantLock lock = paymentLocks.get(paymentId);
        return lock != null && lock.isLocked();
    }

    /**
     * Get the number of currently active locks (for monitoring)
     */
    public int getActiveLockCount() {
        return paymentLocks.size();
    }
} 
package model;

import java.math.BigDecimal;

/**
 * Strategy interface for payment processing.
 * Each concrete strategy (Cash, Card, UPI) encapsulates its own processing logic.
 * PaymentService delegates to whichever strategy the guest chooses.
 */
public interface PaymentMethod {
    Payment process(String paymentId, String bookingId, BigDecimal amount);
    String  getMethodName();
}

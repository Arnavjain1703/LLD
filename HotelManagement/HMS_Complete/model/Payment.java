package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable record of a completed payment transaction.
 * Created by PaymentService and stored against a bookingId.
 */
public class Payment {
    private final String        paymentId;
    private final String        bookingId;
    private final BigDecimal    amount;
    private final String        method;
    private final PaymentStatus status;
    private final LocalDateTime processedAt;

    public Payment(String paymentId, String bookingId,
                   BigDecimal amount, String method, PaymentStatus status) {
        this.paymentId   = paymentId;
        this.bookingId   = bookingId;
        this.amount      = amount;
        this.method      = method;
        this.status      = status;
        this.processedAt = LocalDateTime.now();
    }

    public String        getPaymentId()   { return paymentId; }
    public String        getBookingId()   { return bookingId; }
    public BigDecimal    getAmount()      { return amount; }
    public String        getMethod()      { return method; }
    public PaymentStatus getStatus()      { return status; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    @Override
    public String toString() {
        return "Payment{id=" + paymentId + ", booking=" + bookingId
             + ", amount=" + amount + ", method=" + method
             + ", status=" + status + "}";
    }
}

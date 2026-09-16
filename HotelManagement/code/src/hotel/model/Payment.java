package hotel.model;

import java.time.LocalDateTime;

public class Payment {
    private final String paymentId;
    private final String bookingId;
    private final double amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private final LocalDateTime timestamp;

    public Payment(String paymentId, String bookingId, double amount, PaymentMethod method) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId()     { return paymentId; }
    public String getBookingId()     { return bookingId; }
    public double getAmount()        { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void markSuccess()  { this.status = PaymentStatus.SUCCESS; }
    public void markFailed()   { this.status = PaymentStatus.FAILED; }
    public void markRefunded() { this.status = PaymentStatus.REFUNDED; }

    @Override
    public String toString() {
        return "Payment{id='" + paymentId + "', booking='" + bookingId
                + "', amount=" + amount + ", method=" + method + ", status=" + status + "}";
    }
}

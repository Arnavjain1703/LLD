package model;

import java.math.BigDecimal;

/** UPI payment — simulates payment via UPI ID. */
public class UPIPayment implements PaymentMethod {
    private final String upiId;

    public UPIPayment(String upiId) { this.upiId = upiId; }

    @Override
    public Payment process(String paymentId, String bookingId, BigDecimal amount) {
        System.out.println("[UPIPayment] Sending payment request to " + upiId
                         + " for " + amount + " on booking " + bookingId);
        return new Payment(paymentId, bookingId, amount, getMethodName(), PaymentStatus.SUCCESS);
    }

    @Override
    public String getMethodName() { return "UPI(" + upiId + ")"; }
}

package model;

import java.math.BigDecimal;

/** Cash payment — always succeeds (physical handoff at front desk). */
public class CashPayment implements PaymentMethod {

    @Override
    public Payment process(String paymentId, String bookingId, BigDecimal amount) {
        System.out.println("[CashPayment] Collected " + amount + " in cash for booking " + bookingId);
        return new Payment(paymentId, bookingId, amount, getMethodName(), PaymentStatus.SUCCESS);
    }

    @Override
    public String getMethodName() { return "CASH"; }
}

package model;

import java.math.BigDecimal;

/** Card payment — simulates authorization with card last 4 digits. */
public class CardPayment implements PaymentMethod {
    private final String last4;

    public CardPayment(String last4) { this.last4 = last4; }

    @Override
    public Payment process(String paymentId, String bookingId, BigDecimal amount) {
        System.out.println("[CardPayment] Authorizing card *" + last4
                         + " for " + amount + " on booking " + bookingId);
        // simulate: real impl calls payment gateway
        return new Payment(paymentId, bookingId, amount, getMethodName(), PaymentStatus.SUCCESS);
    }

    @Override
    public String getMethodName() { return "CARD(*" + last4 + ")"; }
}

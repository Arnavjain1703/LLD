package com.bookmyshow.payment;

import java.util.UUID;

public class MockPaymentService implements PaymentService {
    @Override
    public String processPayment(double amount) {
        String id = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[PAYMENT] Rs." + amount + " charged | " + id);
        return id;
    }
    @Override
    public void refund(String paymentId) {
        System.out.println("[PAYMENT] Refunded: " + paymentId);
    }
}

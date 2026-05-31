package com.bookmyshow.payment;

public interface PaymentService {
    String processPayment(double amount);
    void refund(String paymentId);
}

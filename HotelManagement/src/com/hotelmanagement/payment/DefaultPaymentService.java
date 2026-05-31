package com.hotelmanagement.payment;

import com.hotelmanagement.enums.PaymentMethod;
import com.hotelmanagement.enums.PaymentStatus;
import com.hotelmanagement.models.Booking;
import com.hotelmanagement.models.Payment;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default payment implementation with tax calculation and refund logic.
 * Strategy pattern: can be swapped for different payment providers.
 */
public class DefaultPaymentService implements PaymentService {
    private static final double TAX_RATE = 0.18; // 18% GST
    private final AtomicLong paymentCounter = new AtomicLong(5000);

    @Override
    public Payment processPayment(Booking booking, PaymentMethod method) {
        double total = calculateTotal(booking, 0); // basePricePerNight set via booking.totalAmount
        String paymentId = "PAY-" + paymentCounter.incrementAndGet();
        Payment payment = new Payment(paymentId, booking.getId(), booking.getTotalAmount(), method);
        // Simulate payment processing (always succeeds in mock)
        payment.setStatus(PaymentStatus.COMPLETED);
        System.out.println("[PAYMENT] Processed: " + payment);
        return payment;
    }

    @Override
    public Payment processRefund(Booking booking) {
        double refundAmount = calculateRefundAmount(booking);
        String paymentId = "REF-" + paymentCounter.incrementAndGet();
        Payment refund = new Payment(paymentId, booking.getId(), refundAmount, PaymentMethod.UPI);
        refund.setStatus(PaymentStatus.REFUNDED);
        System.out.println("[REFUND] Processed: " + refund);
        return refund;
    }

    @Override
    public double calculateTotal(Booking booking, double basePricePerNight) {
        long nights = booking.getNights();
        double subtotal = nights * basePricePerNight;
        double tax = subtotal * TAX_RATE;
        return subtotal + tax;
    }

    private double calculateRefundAmount(Booking booking) {
        // Full refund if cancelled > 24hrs before check-in, else 50%
        long daysUntilCheckIn = java.time.LocalDate.now().until(booking.getCheckInDate()).getDays();
        if (daysUntilCheckIn > 1) return booking.getTotalAmount();
        return booking.getTotalAmount() * 0.5;
    }
}

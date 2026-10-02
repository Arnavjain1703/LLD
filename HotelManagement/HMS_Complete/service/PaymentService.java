package service;

import model.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PaymentService — payment processing and invoice generation.
 *
 * Strategy pattern: PaymentService.processPayment() delegates to whichever
 * PaymentMethod the guest provides (Cash, Card, UPI). Adding a new payment
 * type requires only a new PaymentMethod implementation — this service
 * does not change.
 *
 * Stores one Payment per booking (bookingId → Payment).
 * generateInvoice() looks up the payment and bundles it with booking details.
 */
public class PaymentService {

    private final Map<String, Payment> paymentStore = new ConcurrentHashMap<>();
    private final AtomicInteger        idSeq        = new AtomicInteger(5000);

    /**
     * Delegates to the PaymentMethod strategy.
     * Stores the result so generateInvoice() can retrieve it later.
     */
    public Payment processPayment(String bookingId,
                                   BigDecimal amount,
                                   PaymentMethod method) {
        String  paymentId = "PAY" + idSeq.getAndIncrement();
        Payment payment   = method.process(paymentId, bookingId, amount);
        paymentStore.put(bookingId, payment);
        System.out.println("[PaymentService] " + payment);
        return payment;
    }

    /**
     * Refund — marks payment as REFUNDED.
     * A new Payment record is created (immutable original is preserved for audit).
     */
    public Payment refund(String bookingId) {
        Payment original = getPaymentForBooking(bookingId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No payment found for booking: " + bookingId));
        Payment refund = new Payment("REF" + idSeq.getAndIncrement(),
                                     bookingId, original.getAmount(),
                                     original.getMethod(), PaymentStatus.REFUNDED);
        paymentStore.put(bookingId + "_refund", refund);
        System.out.println("[PaymentService] Refund issued: " + refund);
        return refund;
    }

    /** Generate invoice at checkout. */
    public Invoice generateInvoice(Booking booking) {
        Payment payment = getPaymentForBooking(booking.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Cannot generate invoice — no payment for booking: "
                + booking.getBookingId()));
        Invoice invoice = new Invoice("INV" + idSeq.getAndIncrement(), booking, payment);
        System.out.println(invoice);
        return invoice;
    }

    public Optional<Payment> getPaymentForBooking(String bookingId) {
        return Optional.ofNullable(paymentStore.get(bookingId));
    }
}

package hotel.service;

import hotel.exception.PaymentFailedException;
import hotel.model.Booking;
import hotel.model.Payment;
import hotel.model.PaymentMethod;
import hotel.repository.PaymentRepository;

import java.util.List;
import java.util.UUID;

public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    public PaymentService(PaymentRepository paymentRepository, BookingService bookingService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    /**
     * Processes payment for a booking.
     * In a real system this would call a payment gateway; here we simulate success.
     */
    public Payment processPayment(String bookingId, PaymentMethod method) {
        Booking booking = bookingService.getBooking(bookingId);
        Payment payment = new Payment(UUID.randomUUID().toString(), bookingId,
                booking.getTotalPrice(), method);
        try {
            // simulate gateway call — always succeeds in this demo
            payment.markSuccess();
        } catch (Exception e) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw new PaymentFailedException("Payment failed for booking " + bookingId + ": " + e.getMessage());
        }
        paymentRepository.save(payment);
        return payment;
    }

    /** Refund the latest successful payment for a booking. */
    public Payment refund(String bookingId) {
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        Payment successful = payments.stream()
                .filter(p -> p.getStatus() == hotel.model.PaymentStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new PaymentFailedException("No successful payment found for booking " + bookingId));
        successful.markRefunded();
        return successful;
    }

    public List<Payment> getPaymentsByBooking(String bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
}

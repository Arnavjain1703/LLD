package hotel.repository;

import hotel.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    void save(Payment payment);
    Optional<Payment> findById(String paymentId);
    List<Payment> findByBookingId(String bookingId);
}

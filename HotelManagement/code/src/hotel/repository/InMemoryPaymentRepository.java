package hotel.repository;

import hotel.model.Payment;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryPaymentRepository implements PaymentRepository {
    private final Map<String, Payment> store = new HashMap<>();

    @Override
    public void save(Payment payment) {
        store.put(payment.getPaymentId(), payment);
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    @Override
    public List<Payment> findByBookingId(String bookingId) {
        return store.values().stream()
                .filter(p -> p.getBookingId().equals(bookingId))
                .collect(Collectors.toList());
    }
}

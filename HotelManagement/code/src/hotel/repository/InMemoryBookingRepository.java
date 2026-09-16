package hotel.repository;

import hotel.model.Booking;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, Booking> store = new HashMap<>();

    @Override
    public void save(Booking booking) {
        store.put(booking.getBookingId(), booking);
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    @Override
    public List<Booking> findByGuestEmail(String email) {
        return store.values().stream()
                .filter(b -> b.getGuestEmail().equals(email))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByRoomId(String roomId) {
        return store.values().stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findActiveByRoom(String roomId) {
        return store.values().stream()
                .filter(b -> b.getRoomId().equals(roomId) && b.isActive())
                .collect(Collectors.toList());
    }
}

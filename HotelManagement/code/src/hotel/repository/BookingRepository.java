package hotel.repository;

import hotel.model.Booking;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    void save(Booking booking);
    Optional<Booking> findById(String bookingId);
    List<Booking> findByGuestEmail(String email);
    List<Booking> findByRoomId(String roomId);
    /** Returns only CONFIRMED or CHECKED_IN bookings for a room. */
    List<Booking> findActiveByRoom(String roomId);
}

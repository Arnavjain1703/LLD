package repository;

import model.Booking;
import model.BookingStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BookingRepository — primary store + two secondary indexes.
 *
 * Primary store:
 *   Map<bookingId, Booking>
 *
 * Secondary indexes:
 *   roomBookingIndex  : Map<roomId,  List<bookingId>>
 *     findActiveByRoomId = O(k) where k = bookings for that room
 *     Without index: O(n) full scan of ALL bookings — unacceptable at scale.
 *
 *   guestBookingIndex : Map<guestId, List<bookingId>>
 *     findByGuestId = O(k) for booking history page.
 *
 * "Active" = PENDING | CONFIRMED | CHECKED_IN
 *   These statuses block the room for new bookings.
 *   CHECKED_OUT and CANCELLED no longer hold the room.
 */
public class BookingRepository {

    private final Map<String, Booking>      store             = new ConcurrentHashMap<>();
    private final Map<String, List<String>> roomBookingIndex  = new ConcurrentHashMap<>();
    private final Map<String, List<String>> guestBookingIndex = new ConcurrentHashMap<>();

    public void save(Booking booking) {
        store.put(booking.getBookingId(), booking);
        roomBookingIndex
            .computeIfAbsent(booking.getRoomId(),  k -> new ArrayList<>())
            .add(booking.getBookingId());
        guestBookingIndex
            .computeIfAbsent(booking.getGuestId(), k -> new ArrayList<>())
            .add(booking.getBookingId());
    }

    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    /**
     * Active bookings for a room.
     * Called by SearchService (every search query) and BookingService
     * (inside the per-room lock before creating a new booking).
     */
    public List<Booking> findActiveByRoomId(String roomId) {
        return roomBookingIndex.getOrDefault(roomId, List.of()).stream()
            .map(store::get)
            .filter(Objects::nonNull)
            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
            .filter(b -> b.getStatus() != BookingStatus.CHECKED_OUT)
            .collect(Collectors.toList());
    }

    /** Full booking history for a guest — all statuses. */
    public List<Booking> findByGuestId(String guestId) {
        return guestBookingIndex.getOrDefault(guestId, List.of()).stream()
            .map(store::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<Booking> findAll() {
        return new ArrayList<>(store.values());
    }
}

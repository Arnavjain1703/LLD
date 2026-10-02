package service;

import model.*;
import repository.BookingRepository;
import repository.HotelRepository;
import repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SearchService — discovery and availability queries.
 *
 * Two levels:
 *   Level 1 — Browse  : getHotelsByCity / getHotelsByMinStars (no date check)
 *   Level 2 — Availability: searchHotels / searchRoomsInHotel
 *
 * Filter chain in getAvailableRooms() (cheapest → most expensive):
 *   1. type filter      O(1)
 *   2. capacity filter  O(1)
 *   3. status check     O(1) — rejects UNDER_MAINTENANCE
 *   4. overlap check    O(k) — hits BookingRepository; goes last
 *
 * Race-condition note:
 *   Search returns a point-in-time snapshot — no lock is held.
 *   A room shown as available may be taken by the time the guest books.
 *   The atomic guard is in BookingService.createBooking() (per-room lock).
 *   Search is eventually consistent; booking is strongly consistent.
 */
public class SearchService {

    private final HotelRepository   hotelRepository;
    private final RoomRepository    roomRepository;
    private final BookingRepository bookingRepository;

    public SearchService(HotelRepository   hotelRepository,
                         RoomRepository    roomRepository,
                         BookingRepository bookingRepository) {
        this.hotelRepository   = hotelRepository;
        this.roomRepository    = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    // ── Level 1: Browse ───────────────────────────────────────────────────

    public List<Hotel> getHotelsByCity(String city) {
        return hotelRepository.findByCity(city);
    }

    public List<Hotel> getHotelsByMinStars(int minStars) {
        return hotelRepository.findByMinStarRating(minStars);
    }

    // ── Level 2: Availability ─────────────────────────────────────────────

    public List<SearchResult> searchHotels(SearchCriteria criteria) {
        return hotelRepository.findByCity(criteria.getCity()).stream()
            .map(hotel -> new SearchResult(hotel,
                             getAvailableRooms(hotel.getHotelId(), criteria)))
            .filter(r -> r.getRoomCount() > 0)
            .collect(Collectors.toList());
    }

    public List<Room> searchRoomsInHotel(String hotelId, SearchCriteria criteria) {
        return getAvailableRooms(hotelId, criteria);
    }

    /** Point check — called by BookingService before creating a booking. */
    public boolean isRoomAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE) return false;
        return bookingRepository.findActiveByRoomId(roomId).stream()
            .noneMatch(b -> b.overlaps(checkIn, checkOut));
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private List<Room> getAvailableRooms(String hotelId, SearchCriteria criteria) {
        return roomRepository.findByHotelId(hotelId).stream()
            .filter(r -> criteria.getRoomType() == null
                      || r.getType() == criteria.getRoomType())
            .filter(r -> r.getCapacity() >= criteria.getMinCapacity())
            .filter(r -> r.getStatus() != RoomStatus.UNDER_MAINTENANCE)
            .filter(r -> hasNoOverlappingBooking(r.getRoomId(),
                            criteria.getCheckIn(), criteria.getCheckOut()))
            .collect(Collectors.toList());
    }

    private boolean hasNoOverlappingBooking(String roomId,
                                             LocalDate checkIn, LocalDate checkOut) {
        return bookingRepository.findActiveByRoomId(roomId).stream()
            .noneMatch(b -> b.overlaps(checkIn, checkOut));
    }
}

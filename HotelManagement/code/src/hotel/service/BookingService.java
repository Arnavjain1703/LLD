package hotel.service;

import hotel.exception.BookingConflictException;
import hotel.exception.BookingNotFoundException;
import hotel.model.*;
import hotel.pricing.PricingStrategy;
import hotel.pricing.StandardPricingStrategy;
import hotel.repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BookingService {
    private final BookingRepository bookingRepository;
    private final HotelService hotelService;
    private final GuestService guestService;
    private PricingStrategy pricingStrategy;

    public BookingService(BookingRepository bookingRepository,
                          HotelService hotelService,
                          GuestService guestService) {
        this.bookingRepository = bookingRepository;
        this.hotelService = hotelService;
        this.guestService = guestService;
        this.pricingStrategy = new StandardPricingStrategy();
    }

    /** Swap pricing strategy at runtime (Strategy pattern). */
    public void setPricingStrategy(PricingStrategy strategy) {
        this.pricingStrategy = strategy;
    }

    public Booking bookRoom(String guestEmail, String roomId, LocalDate checkIn, LocalDate checkOut) {
        // validate guest exists
        guestService.getGuest(guestEmail);

        // validate room exists
        Room room = hotelService.getRoom(roomId);

        // check for date conflicts on this room
        validateNoOverlap(roomId, checkIn, checkOut);

        double price = pricingStrategy.calculatePrice(room, checkIn, checkOut);
        String bookingId = UUID.randomUUID().toString();
        Booking booking = new Booking(bookingId, guestEmail, roomId, room.getHotelId(),
                checkIn, checkOut, price);

        // mark the room as booked
        room.book();
        bookingRepository.save(booking);
        return booking;
    }

    public void cancelBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        if (!booking.isActive()) {
            throw new IllegalStateException("Cannot cancel booking in status: " + booking.getStatus());
        }
        booking.cancel();
        // free the room only if it was BOOKED (not CHECKED_IN — guest is still there)
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Room room = hotelService.getRoom(booking.getRoomId());
            room.checkOut(); // resets to AVAILABLE via state transition; use checkOut for simplicity
        }
    }

    public void checkIn(String bookingId) {
        Booking booking = getBooking(bookingId);
        booking.checkIn();
        Room room = hotelService.getRoom(booking.getRoomId());
        room.checkIn();
    }

    public void checkOut(String bookingId) {
        Booking booking = getBooking(bookingId);
        booking.checkOut();
        Room room = hotelService.getRoom(booking.getRoomId());
        room.checkOut();
    }

    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
    }

    public List<Booking> getBookingsByGuest(String email) {
        return bookingRepository.findByGuestEmail(email);
    }

    // --- private helpers ---

    private void validateNoOverlap(String roomId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> active = bookingRepository.findActiveByRoom(roomId);
        boolean conflict = active.stream().anyMatch(b -> b.overlaps(checkIn, checkOut));
        if (conflict) {
            throw new BookingConflictException(
                    "Room " + roomId + " is already booked for the requested dates");
        }
    }
}

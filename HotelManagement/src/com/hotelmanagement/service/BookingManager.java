package com.hotelmanagement.service;

import com.hotelmanagement.enums.BookingStatus;
import com.hotelmanagement.enums.RoomStatus;
import com.hotelmanagement.exceptions.InvalidBookingException;
import com.hotelmanagement.exceptions.RoomNotAvailableException;
import com.hotelmanagement.models.Booking;
import com.hotelmanagement.models.Room;

import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages booking lifecycle with concurrent safety.
 * - AtomicLong for lock-free ID generation
 * - ScheduledExecutorService for automatic hold expiry (15-min TTL)
 * - ConcurrentHashMap for O(1) booking lookup
 */
public class BookingManager {
    private final ConcurrentHashMap<String, Booking> bookings;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> holdTimers;
    private final AtomicLong bookingCounter;
    private final ScheduledExecutorService scheduler;
    private final RoomManager roomManager;

    private static final long HOLD_TTL_MINUTES = 15;

    public BookingManager(RoomManager roomManager) {
        this.bookings = new ConcurrentHashMap<>();
        this.holdTimers = new ConcurrentHashMap<>();
        this.bookingCounter = new AtomicLong(1000);
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.roomManager = roomManager;
    }

    /**
     * Creates a booking with a 15-minute hold.
     * Room is atomically allocated via RoomManager (per-room lock).
     * If allocation fails, throws RoomNotAvailableException.
     */
    public Booking createBooking(String guestId, String roomId, String hotelId,
                                 LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);

        // Atomic room allocation (uses per-room ReentrantLock internally)
        boolean allocated = roomManager.allocateRoom(roomId, checkIn, checkOut);
        if (!allocated) {
            throw new RoomNotAvailableException(
                    "Room " + roomId + " not available for " + checkIn + " to " + checkOut);
        }

        String bookingId = "BKG-" + bookingCounter.incrementAndGet();
        Booking booking = new Booking(bookingId, guestId, roomId, hotelId, checkIn, checkOut);
        bookings.put(bookingId, booking);

        // Schedule auto-cancellation after TTL
        scheduleHoldExpiry(bookingId);
        return booking;
    }

    /**
     * Confirms a pending booking (called after successful payment).
     * Cancels the hold expiry timer.
     */
    public void confirmBooking(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingException("Cannot confirm booking in state: " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        cancelHoldTimer(bookingId);

        // Update room status if check-in is today
        Room room = roomManager.getRoom(booking.getRoomId());
        if (room != null) room.setStatus(RoomStatus.RESERVED);
    }

    public void cancelBooking(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new InvalidBookingException("Cannot cancel after check-in");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) return;

        booking.setStatus(BookingStatus.CANCELLED);
        cancelHoldTimer(bookingId);
        roomManager.releaseRoom(booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate());

        Room room = roomManager.getRoom(booking.getRoomId());
        if (room != null) room.setStatus(RoomStatus.AVAILABLE);
    }

    public void checkIn(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingException("Cannot check-in booking in state: " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        Room room = roomManager.getRoom(booking.getRoomId());
        if (room != null) room.setStatus(RoomStatus.OCCUPIED);
    }

    public void checkOut(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new InvalidBookingException("Cannot check-out booking in state: " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CHECKED_OUT);
        roomManager.releaseRoom(booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate());

        Room room = roomManager.getRoom(booking.getRoomId());
        if (room != null) room.setStatus(RoomStatus.AVAILABLE);
    }

    public Booking getBooking(String bookingId) { return bookings.get(bookingId); }

    private void scheduleHoldExpiry(String bookingId) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            Booking booking = bookings.get(bookingId);
            if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                System.out.println("[HOLD EXPIRED] Auto-cancelling booking: " + bookingId);
                cancelBooking(bookingId);
            }
        }, HOLD_TTL_MINUTES, TimeUnit.MINUTES);
        holdTimers.put(bookingId, future);
    }

    private void cancelHoldTimer(String bookingId) {
        ScheduledFuture<?> future = holdTimers.remove(bookingId);
        if (future != null) future.cancel(false);
    }

    private Booking getBookingOrThrow(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) throw new InvalidBookingException("Booking not found: " + bookingId);
        return booking;
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null)
            throw new InvalidBookingException("Dates cannot be null");
        if (!checkOut.isAfter(checkIn))
            throw new InvalidBookingException("Check-out must be after check-in");
        if (checkIn.isBefore(LocalDate.now()))
            throw new InvalidBookingException("Check-in cannot be in the past");
        if (checkIn.until(checkOut).getDays() > 30)
            throw new InvalidBookingException("Maximum booking duration is 30 nights");
    }

    public void shutdown() { scheduler.shutdown(); }
}

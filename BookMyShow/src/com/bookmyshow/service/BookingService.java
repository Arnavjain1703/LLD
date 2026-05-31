package com.bookmyshow.service;

import com.bookmyshow.enums.SeatStatus;
import com.bookmyshow.exceptions.SeatNotAvailableException;
import com.bookmyshow.models.*;
import com.bookmyshow.observer.NotificationService;
import com.bookmyshow.payment.PaymentService;
import com.bookmyshow.strategy.CancellationPolicy;
import com.bookmyshow.strategy.PricingStrategy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton + Facade.
 * All data stored inline (ConcurrentHashMaps) — no separate repository layer.
 * Thread-safety: per-show ReentrantLock for seat operations.
 */
public class BookingService {

    // ── Singleton (volatile + DCL) ────────────────────────────────────────
    private static volatile BookingService INSTANCE;

    public static BookingService getInstance(PricingStrategy pricing,
                                             CancellationPolicy cancellation,
                                             PaymentService payment,
                                             List<NotificationService> observers) {
        if (INSTANCE == null) {
            synchronized (BookingService.class) {
                if (INSTANCE == null)
                    INSTANCE = new BookingService(pricing, cancellation, payment, observers);
            }
        }
        return INSTANCE;
    }
    public static void resetInstance() { INSTANCE = null; }

    // ── Inline data stores (replaces repositories) ────────────────────────
    private final Map<String, Show>     shows    = new ConcurrentHashMap<>();
    private final Map<String, Booking>  bookings = new ConcurrentHashMap<>();
    private final Map<String, SeatLock> locks    = new ConcurrentHashMap<>();

    // ── Per-show lock for thread safety ───────────────────────────────────
    private final ConcurrentHashMap<String, ReentrantLock> showLocks = new ConcurrentHashMap<>();
    private static final int LOCK_TTL_MINUTES = 10;

    // ── Strategy + Observer dependencies ──────────────────────────────────
    private final PricingStrategy      pricingStrategy;
    private final CancellationPolicy   cancellationPolicy;
    private final PaymentService       paymentService;
    private final List<NotificationService> observers;

    private BookingService(PricingStrategy pricing, CancellationPolicy cancellation,
                           PaymentService payment, List<NotificationService> observers) {
        this.pricingStrategy    = pricing;
        this.cancellationPolicy = cancellation;
        this.paymentService     = payment;
        this.observers          = observers;
    }

    // ── Register a show (called during setup) ─────────────────────────────
    public void registerShow(Show show) {
        shows.put(show.getId(), show);
    }

    // ── Get seat map for a show ───────────────────────────────────────────
    public Map<String, SeatStatus> getSeatMap(String showId) {
        expireStaleLocksFor(showId);
        Show show = shows.get(showId);
        return show != null ? new HashMap<>(show.getSeatStatusMap()) : Map.of();
    }

    // ── Lock Seats ────────────────────────────────────────────────────────
    public SeatLock lockSeats(String showId, List<String> seatIds, String userId) {
        expireStaleLocksFor(showId);
        Show show = shows.get(showId);
        if (show == null) throw new IllegalArgumentException("Show not found: " + showId);

        ReentrantLock lock = showLocks.computeIfAbsent(showId, k -> new ReentrantLock());
        lock.lock();
        try {
            // Atomic check-then-set: ALL seats must be AVAILABLE
            for (String seatId : seatIds) {
                SeatStatus status = show.getSeatStatus(seatId);
                if (status != SeatStatus.AVAILABLE)
                    throw new SeatNotAvailableException("Seat " + seatId + " is " + status);
            }
            seatIds.forEach(id -> show.updateSeatStatus(id, SeatStatus.LOCKED));
        } finally {
            lock.unlock();
        }

        String lockId = "LCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SeatLock seatLock = new SeatLock(lockId, showId, seatIds, userId, LOCK_TTL_MINUTES);
        locks.put(lockId, seatLock);

        System.out.println("[LOCK] " + seatIds + " for " + userId + " | " + lockId);
        return seatLock;
    }

    // ── Confirm Booking ───────────────────────────────────────────────────
    public Booking confirmBooking(String lockId) {
        SeatLock seatLock = locks.get(lockId);
        if (seatLock == null) throw new IllegalArgumentException("Lock not found: " + lockId);

        Show show = shows.get(seatLock.getShowId());

        if (seatLock.isExpired()) {
            releaseSeats(show, seatLock.getSeatIds());
            locks.remove(lockId);
            throw new IllegalStateException("Lock expired: " + lockId);
        }

        List<Seat> seats = resolveSeats(show, seatLock.getSeatIds());
        double price = pricingStrategy.calculatePrice(seats);

        // Payment
        String paymentId = paymentService.processPayment(price);

        // Mark BOOKED (under lock)
        ReentrantLock showLock = showLocks.computeIfAbsent(seatLock.getShowId(), k -> new ReentrantLock());
        showLock.lock();
        try {
            seatLock.getSeatIds().forEach(id -> show.updateSeatStatus(id, SeatStatus.BOOKED));
        } finally {
            showLock.unlock();
        }

        // Create booking (Factory inlined)
        String bookingId = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = new Booking(bookingId, seatLock.getUserId(), show, seats, price);
        booking.confirm(paymentId);
        bookings.put(bookingId, booking);
        locks.remove(lockId);

        // Notify observers
        observers.forEach(o -> o.onBookingConfirmed(booking));
        System.out.println("[CONFIRMED] " + bookingId + " | Rs." + price);
        return booking;
    }

    // ── Cancel Booking ────────────────────────────────────────────────────
    public Booking cancelBooking(String bookingId, String userId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) throw new IllegalArgumentException("Booking not found: " + bookingId);
        if (!booking.getUserId().equals(userId))
            throw new IllegalArgumentException("Not your booking");
        if (!cancellationPolicy.canCancel(booking))
            throw new IllegalStateException("Cancellation window closed");

        Show show = booking.getShow();
        ReentrantLock showLock = showLocks.computeIfAbsent(show.getId(), k -> new ReentrantLock());
        showLock.lock();
        try {
            booking.getSeats().forEach(s -> show.updateSeatStatus(s.getId(), SeatStatus.AVAILABLE));
        } finally {
            showLock.unlock();
        }

        paymentService.refund(booking.getPaymentId());
        booking.cancel();

        observers.forEach(o -> o.onBookingCancelled(booking));
        System.out.println("[CANCELLED] " + bookingId);
        return booking;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void releaseSeats(Show show, List<String> seatIds) {
        seatIds.forEach(id -> show.updateSeatStatus(id, SeatStatus.AVAILABLE));
    }

    private List<Seat> resolveSeats(Show show, List<String> seatIds) {
        List<Seat> result = new ArrayList<>();
        for (Seat[] row : show.getScreen().getSeatLayout())
            for (Seat seat : row)
                if (seatIds.contains(seat.getId())) result.add(seat);
        return result;
    }

    private void expireStaleLocksFor(String showId) {
        Show show = shows.get(showId);
        if (show == null) return;
        locks.values().removeIf(lock -> {
            if (lock.getShowId().equals(showId) && lock.isExpired()) {
                lock.getSeatIds().forEach(id -> show.updateSeatStatus(id, SeatStatus.AVAILABLE));
                return true;
            }
            return false;
        });
    }
}

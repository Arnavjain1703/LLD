package service;

import model.*;
import repository.BookingRepository;
import repository.GuestRepository;
import repository.RoomRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BookingService — full booking lifecycle with FSM, race-condition guard,
 * payment processing, and notifications.
 *
 * ── FSM ──────────────────────────────────────────────────────────────────────
 *   PENDING    --> CONFIRMED   (confirmBooking  — payment taken)
 *   PENDING    --> CANCELLED   (cancelBooking   — refund not applicable)
 *   CONFIRMED  --> CHECKED_IN  (checkIn)
 *   CONFIRMED  --> CANCELLED   (cancelBooking   — refund issued)
 *   CHECKED_IN --> CHECKED_OUT (checkOut        — invoice generated)
 *   CHECKED_OUT, CANCELLED are terminal states.
 *
 * ── Race Condition Fix ────────────────────────────────────────────────────────
 *   Problem (TOCTOU): two guests both see room R101 as available via search,
 *   both call createBooking() concurrently — without a guard both save.
 *
 *   Fix: per-room lock. ConcurrentHashMap<roomId, Object>.
 *   The overlap-check + save block is wrapped in synchronized(lock).
 *   Atomic for each room independently — R101 and R201 bookings run in parallel.
 *
 *   Thread A: acquire lock(R101) → overlap check passes → save → release
 *   Thread B: blocked on lock(R101) → acquires → overlap found → throws
 *   Thread C: acquire lock(R201) — runs in parallel with A & B, unblocked
 */
public class BookingService {

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS;
    static {
        Map<BookingStatus, Set<BookingStatus>> m = new HashMap<>();
        m.put(BookingStatus.PENDING,     EnumSet.of(BookingStatus.CONFIRMED,
                                                    BookingStatus.CANCELLED));
        m.put(BookingStatus.CONFIRMED,   EnumSet.of(BookingStatus.CHECKED_IN,
                                                    BookingStatus.CANCELLED));
        m.put(BookingStatus.CHECKED_IN,  EnumSet.of(BookingStatus.CHECKED_OUT));
        m.put(BookingStatus.CHECKED_OUT, EnumSet.noneOf(BookingStatus.class));
        m.put(BookingStatus.CANCELLED,   EnumSet.noneOf(BookingStatus.class));
        VALID_TRANSITIONS = Collections.unmodifiableMap(m);
    }

    private final BookingRepository     bookingRepository;
    private final RoomRepository        roomRepository;
    private final GuestRepository       guestRepository;
    private final PaymentService        paymentService;
    private final NotificationService   notificationService;

    private final ConcurrentHashMap<String, Object> roomLocks = new ConcurrentHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(1000);

    public BookingService(BookingRepository   bookingRepository,
                          RoomRepository      roomRepository,
                          GuestRepository     guestRepository,
                          PaymentService      paymentService,
                          NotificationService notificationService) {
        this.bookingRepository   = bookingRepository;
        this.roomRepository      = roomRepository;
        this.guestRepository     = guestRepository;
        this.paymentService      = paymentService;
        this.notificationService = notificationService;
    }

    /**
     * Create booking in PENDING state.
     * Per-room lock makes overlap-check + save atomic.
     */
    public Booking createBooking(String guestId, String hotelId, String roomId,
                                  LocalDate checkIn, LocalDate checkOut) {
        guestRepository.findById(guestId)
            .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestId));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE)
            throw new IllegalStateException("Room is under maintenance: " + roomId);

        Object lock = roomLocks.computeIfAbsent(roomId, k -> new Object());
        synchronized (lock) {
            boolean conflict = bookingRepository.findActiveByRoomId(roomId).stream()
                .anyMatch(b -> b.overlaps(checkIn, checkOut));
            if (conflict)
                throw new IllegalStateException(
                    "Room " + roomId + " already booked for requested dates.");

            String  bookingId = "BK" + idSeq.getAndIncrement();
            Booking booking   = new Booking(bookingId, guestId, hotelId, roomId,
                                            checkIn, checkOut, room.getPricePerNight());
            bookingRepository.save(booking);
            System.out.println("[createBooking] " + booking);
            return booking;
        }
    }

    /**
     * PENDING -> CONFIRMED.
     * Processes payment via strategy; notifies guest.
     * If payment fails, booking stays PENDING — guest can retry.
     */
    public Booking confirmBooking(String bookingId, PaymentMethod paymentMethod) {
        Booking booking = getBooking(bookingId);
        Payment payment = paymentService.processPayment(
            bookingId, booking.getTotalAmount(), paymentMethod);

        if (payment.getStatus() != PaymentStatus.SUCCESS)
            throw new IllegalStateException("Payment failed for booking: " + bookingId);

        transition(booking, BookingStatus.CONFIRMED);

        guestRepository.findById(booking.getGuestId())
            .ifPresent(g -> notificationService.notifyBookingConfirmed(booking, g));

        return booking;
    }

    /** CONFIRMED -> CHECKED_IN. Notifies guest. */
    public Booking checkIn(String bookingId) {
        Booking booking = getBooking(bookingId);
        transition(booking, BookingStatus.CHECKED_IN);
        guestRepository.findById(booking.getGuestId())
            .ifPresent(g -> notificationService.notifyCheckIn(booking, g));
        return booking;
    }

    /**
     * CHECKED_IN -> CHECKED_OUT.
     * Releases room. Generates invoice. Notifies guest.
     */
    public Booking checkOut(String bookingId) {
        Booking booking = getBooking(bookingId);
        transition(booking, BookingStatus.CHECKED_OUT);

        roomRepository.findById(booking.getRoomId())
            .ifPresent(r -> r.setStatus(RoomStatus.AVAILABLE));

        Invoice invoice = paymentService.generateInvoice(booking);

        guestRepository.findById(booking.getGuestId())
            .ifPresent(g -> notificationService.notifyCheckOut(booking, g, invoice));

        return booking;
    }

    /**
     * PENDING or CONFIRMED -> CANCELLED.
     * Refund issued if booking was CONFIRMED.
     */
    public Booking cancelBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;

        transition(booking, BookingStatus.CANCELLED);

        if (wasConfirmed) paymentService.refund(bookingId);

        guestRepository.findById(booking.getGuestId())
            .ifPresent(g -> notificationService.notifyCancellation(booking, g));

        return booking;
    }

    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    public List<Booking> getBookingHistory(String guestId) {
        return bookingRepository.findByGuestId(guestId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    private void transition(Booking booking, BookingStatus target) {
        Set<BookingStatus> allowed = VALID_TRANSITIONS.get(booking.getStatus());
        if (!allowed.contains(target))
            throw new IllegalStateException(
                "Invalid FSM transition: " + booking.getBookingId()
                + " [" + booking.getStatus() + " -> " + target + "]");
        booking.setStatus(target);
    }
}

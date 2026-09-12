package library.service;

import library.exception.BookNotFoundException;
import library.exception.MemberNotActiveException;
import library.exception.ReservationNotFoundException;
import library.model.Book;
import library.model.BookReservation;
import library.model.Member;
import library.model.ReservationStatus;
import library.notification.NotificationDispatcher;
import library.repository.BookRepository;
import library.reservation.ReservationQueue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the reservation lifecycle:
 *   reserve()      — member joins the FIFO waiting queue for an ISBN
 *   cancel()       — member leaves the queue before being notified
 *   notifyNext()   — called by ReturnService when a copy becomes available
 *   expireStale()  — called by a scheduler; expires NOTIFIED reservations
 *                    whose collection window has passed
 */
public class ReservationService {

    // Collection window after notification — member must pick up within this time
    private static final long EXPIRY_HOURS = 72;

    private final ReservationQueue       reservationQueue;
    private final NotificationDispatcher notificationDispatcher;
    private final BookRepository         bookRepository;

    public ReservationService(ReservationQueue reservationQueue,
                              NotificationDispatcher notificationDispatcher,
                              BookRepository bookRepository) {
        this.reservationQueue       = reservationQueue;
        this.notificationDispatcher = notificationDispatcher;
        this.bookRepository         = bookRepository;
    }

    // ── Reserve ───────────────────────────────────────────────────────────────

    /**
     * Place a member in the FIFO waiting queue for the given ISBN.
     * Validates member is ACTIVE — suspended/blacklisted members cannot reserve.
     */
    public BookReservation reserve(Member member, String isbn) {
        if (!member.canBorrow()) {
            throw new MemberNotActiveException(member.getEmail());
        }
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException(isbn));

        BookReservation reservation = new BookReservation(book, member);
        reservationQueue.enqueue(reservation);

        notificationDispatcher.dispatch(member,
                "You are on the waiting list for \"" + book.getTitle()
                + "\". We will notify you when a copy is available.");

        return reservation;
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    /** Member cancels their reservation before collection. */
    public void cancel(String reservationId) {
        BookReservation reservation = findActiveReservation(reservationId);
        reservation.cancel();
        reservationQueue.remove(reservationId);

        notificationDispatcher.dispatch(reservation.getMember(),
                "Your reservation for \"" + reservation.getBook().getTitle()
                + "\" has been cancelled.");
    }

    // ── Notify next in queue ──────────────────────────────────────────────────

    /**
     * Called by ReturnService when a copy of the given ISBN is returned.
     * Notifies the head of the waiting queue that a copy is now available.
     */
    public void notifyNext(String isbn) {
        reservationQueue.peek(isbn).ifPresent(reservation -> {
            reservation.markNotified();
            notificationDispatcher.dispatch(reservation.getMember(),
                    "Good news! \"" + reservation.getBook().getTitle()
                    + "\" is now available for you. Please collect within "
                    + EXPIRY_HOURS + " hours.");
        });
    }

    // ── Expire stale ─────────────────────────────────────────────────────────

    /**
     * Expires any NOTIFIED reservations whose collection window has passed.
     * Intended to be called by a scheduled background job.
     */
    public void expireStale() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(EXPIRY_HOURS);

        List<BookReservation> stale = reservationQueue.getAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.NOTIFIED)
                .filter(r -> r.getNotifiedAt() != null && r.getNotifiedAt().isBefore(cutoff))
                .collect(Collectors.toList());

        stale.forEach(r -> {
            r.expire();
            reservationQueue.remove(r.getReservationId());
            notificationDispatcher.dispatch(r.getMember(),
                    "Your reservation for \"" + r.getBook().getTitle()
                    + "\" has expired. Please reserve again if still needed.");
            // notify the next person in queue since this slot opened up
            notifyNext(r.getBook().getIsbn());
        });
    }

    /** Delegate — used by ReturnService to decide if a copy should be RESERVED. */
    public boolean hasWaiting(String isbn) {
        return reservationQueue.hasWaiting(isbn);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BookReservation findActiveReservation(String reservationId) {
        return reservationQueue.getAll().stream()
                .filter(r -> r.getReservationId().equals(reservationId) && r.isActive())
                .findFirst()
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}

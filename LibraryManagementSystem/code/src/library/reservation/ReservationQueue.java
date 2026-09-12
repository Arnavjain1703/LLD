package library.reservation;

import library.model.BookReservation;
import library.model.ReservationStatus;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Per-ISBN FIFO waiting queue for reservations.
 * Only WAITING reservations stay in the active queue.
 * All mutations are synchronized to prevent concurrent enqueue/dequeue races.
 */
public class ReservationQueue {

    // isbn → FIFO queue of active (WAITING) reservations
    private final Map<String, Queue<BookReservation>> queues = new ConcurrentHashMap<>();

    public synchronized void enqueue(BookReservation reservation) {
        queues.computeIfAbsent(reservation.getBook().getIsbn(), k -> new ArrayDeque<>())
              .add(reservation);
    }

    /** Remove and return the head WAITING reservation for the given ISBN. */
    public synchronized Optional<BookReservation> dequeue(String isbn) {
        Queue<BookReservation> q = queues.get(isbn);
        if (q == null || q.isEmpty()) return Optional.empty();
        return Optional.ofNullable(q.poll());
    }

    /** Peek at the head without removing. */
    public Optional<BookReservation> peek(String isbn) {
        Queue<BookReservation> q = queues.get(isbn);
        if (q == null || q.isEmpty()) return Optional.empty();
        return Optional.ofNullable(q.peek());
    }

    /** Remove a reservation by id — used on cancel or expire. */
    public synchronized void remove(String reservationId) {
        queues.values().forEach(q ->
                q.removeIf(r -> r.getReservationId().equals(reservationId)));
    }

    public boolean hasWaiting(String isbn) {
        Queue<BookReservation> q = queues.get(isbn);
        return q != null && !q.isEmpty();
    }

    /** All reservations across all ISBNs — used by expireStale(). */
    public List<BookReservation> getAll() {
        return queues.values().stream()
                .flatMap(Queue::stream)
                .collect(Collectors.toList());
    }
}

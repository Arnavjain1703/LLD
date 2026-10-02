package model;

/**
 * FSM states for a Booking.
 *
 * Valid transitions:
 *   PENDING    --> CONFIRMED  (payment taken)
 *   PENDING    --> CANCELLED
 *   CONFIRMED  --> CHECKED_IN
 *   CONFIRMED  --> CANCELLED
 *   CHECKED_IN --> CHECKED_OUT
 *   CHECKED_OUT, CANCELLED are terminal.
 *
 * Race-condition note:
 *   Two threads can both see a room as AVAILABLE via search, then race to
 *   call createBooking(). The per-room lock in BookingService makes the
 *   overlap-check + save atomic — only one thread wins, the other gets
 *   an IllegalStateException.
 *
 *   For better UX (avoid filling payment form and then failing), promote
 *   PENDING to a short-TTL HOLD state created at "Book Now" click, before
 *   payment. The HOLD blocks other bookings for 10 minutes, then auto-expires.
 */
public enum BookingStatus {
    PENDING,       // created, awaiting payment
    CONFIRMED,     // payment taken, awaiting check-in
    CHECKED_IN,    // guest has arrived
    CHECKED_OUT,   // stay complete (terminal)
    CANCELLED      // cancelled by guest or hotel (terminal)
}

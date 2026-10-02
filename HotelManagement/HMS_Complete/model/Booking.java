package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Booking — a reservation of one room for a guest over a date range.
 *
 * Design decisions:
 *   - Single roomId (not List): one booking = one room. Multi-room stays
 *     create multiple Booking objects. Keeps FSM and concurrency simple —
 *     each booking is locked and transitioned independently.
 *   - totalAmount locked at creation: pricePerNight * nights. Price changes
 *     after booking do not affect existing reservations.
 *   - createdAt for audit trail and TTL-based hold expiry.
 */
public class Booking {

    private final String        bookingId;
    private final String        guestId;
    private final String        hotelId;
    private final String        roomId;         // single room per booking
    private final LocalDate     checkIn;
    private final LocalDate     checkOut;
    private final BigDecimal    totalAmount;    // locked at creation time
    private final LocalDateTime createdAt;
    private       BookingStatus status;

    public Booking(String bookingId, String guestId, String hotelId, String roomId,
                   LocalDate checkIn, LocalDate checkOut, BigDecimal pricePerNight) {
        if (!checkOut.isAfter(checkIn))
            throw new IllegalArgumentException("Check-out must be after check-in");
        this.bookingId   = bookingId;
        this.guestId     = guestId;
        this.hotelId     = hotelId;
        this.roomId      = roomId;
        this.checkIn     = checkIn;
        this.checkOut    = checkOut;
        this.totalAmount = pricePerNight.multiply(BigDecimal.valueOf(getNights()));
        this.createdAt   = LocalDateTime.now();
        this.status      = BookingStatus.PENDING;
    }

    public String        getBookingId()   { return bookingId; }
    public String        getGuestId()     { return guestId; }
    public String        getHotelId()     { return hotelId; }
    public String        getRoomId()      { return roomId; }
    public LocalDate     getCheckIn()     { return checkIn; }
    public LocalDate     getCheckOut()    { return checkOut; }
    public BigDecimal    getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public BookingStatus getStatus()      { return status; }

    public void setStatus(BookingStatus status) { this.status = status; }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Core overlap check — used by SearchService and BookingService.
     *
     * Overlap condition: thisStart < otherEnd AND thisEnd > otherStart
     * Adjacent dates are NOT an overlap:
     *   Existing Jun5->Jun10, Requested Jun10->Jun15 → allowed.
     */
    public boolean overlaps(LocalDate reqCheckIn, LocalDate reqCheckOut) {
        return this.checkIn.isBefore(reqCheckOut)
            && this.checkOut.isAfter(reqCheckIn);
    }

    @Override
    public String toString() {
        return "Booking{id=" + bookingId + ", guest=" + guestId
             + ", room=" + roomId + ", " + checkIn + "->" + checkOut
             + ", nights=" + getNights() + ", total=" + totalAmount
             + ", status=" + status + "}";
    }
}

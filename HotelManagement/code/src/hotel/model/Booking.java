package hotel.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Booking {
    private final String bookingId;
    private final String guestEmail;
    private final String roomId;
    private final String hotelId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final double totalPrice;
    private BookingStatus status;

    public Booking(String bookingId, String guestEmail, String roomId, String hotelId,
                   LocalDate checkInDate, LocalDate checkOutDate, double totalPrice) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        this.bookingId = bookingId;
        this.guestEmail = guestEmail;
        this.roomId = roomId;
        this.hotelId = hotelId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getBookingId()       { return bookingId; }
    public String getGuestEmail()      { return guestEmail; }
    public String getRoomId()          { return roomId; }
    public String getHotelId()         { return hotelId; }
    public LocalDate getCheckInDate()  { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public double getTotalPrice()      { return totalPrice; }
    public BookingStatus getStatus()   { return status; }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /** True if [checkIn, checkOut) overlaps with this booking's window. */
    public boolean overlaps(LocalDate checkIn, LocalDate checkOut) {
        // overlap when: checkIn < this.checkOut AND checkOut > this.checkInDate
        return checkIn.isBefore(checkOutDate) && checkOut.isAfter(checkInDate);
    }

    public boolean isActive() {
        return status == BookingStatus.CONFIRMED || status == BookingStatus.CHECKED_IN;
    }

    public void cancel()    { this.status = BookingStatus.CANCELLED; }
    public void checkIn()   { this.status = BookingStatus.CHECKED_IN; }
    public void checkOut()  { this.status = BookingStatus.CHECKED_OUT; }

    @Override
    public String toString() {
        return "Booking{id='" + bookingId + "', guest='" + guestEmail + "', room='" + roomId
                + "', " + checkInDate + " → " + checkOutDate + ", status=" + status + "}";
    }
}

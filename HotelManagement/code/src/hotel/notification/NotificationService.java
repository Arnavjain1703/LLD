package hotel.notification;

import hotel.model.Booking;
import hotel.model.Guest;

/** Stub notification service — prints to console in lieu of email/SMS gateway. */
public class NotificationService {

    public void sendBookingConfirmation(Guest guest, Booking booking) {
        System.out.println("[NOTIFICATION] Booking confirmed for " + guest.getName()
                + " | BookingId: " + booking.getBookingId()
                + " | " + booking.getCheckInDate() + " → " + booking.getCheckOutDate()
                + " | Total: $" + String.format("%.2f", booking.getTotalPrice()));
    }

    public void sendCancellationNotice(Guest guest, Booking booking) {
        System.out.println("[NOTIFICATION] Booking cancelled for " + guest.getName()
                + " | BookingId: " + booking.getBookingId());
    }

    public void sendCheckInReminder(Guest guest, Booking booking) {
        System.out.println("[NOTIFICATION] Check-in reminder for " + guest.getName()
                + " | BookingId: " + booking.getBookingId()
                + " | Check-in: " + booking.getCheckInDate());
    }

    public void sendPaymentReceipt(Guest guest, hotel.model.Payment payment) {
        System.out.println("[NOTIFICATION] Payment receipt for " + guest.getName()
                + " | Amount: $" + String.format("%.2f", payment.getAmount())
                + " | Method: " + payment.getMethod()
                + " | Status: " + payment.getStatus());
    }
}

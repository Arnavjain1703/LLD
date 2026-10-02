package service;

import model.*;

import java.util.List;

/**
 * NotificationService — broadcasts booking lifecycle events to all channels.
 *
 * Holds a list of NotificationChannel implementations (Email, SMS, ...).
 * Every event is sent to ALL channels — Observer pattern.
 * Adding a new channel (Push, WhatsApp) = add to the list; this class stays unchanged.
 */
public class NotificationService {

    private final List<NotificationChannel> channels;

    public NotificationService(List<NotificationChannel> channels) {
        this.channels = List.copyOf(channels);
    }

    public void notifyBookingConfirmed(Booking booking, Guest guest) {
        broadcast(
            guest.getEmail(),
            "Booking Confirmed: " + booking.getBookingId(),
            "Dear " + guest.getName() + ", your booking " + booking.getBookingId()
            + " for room " + booking.getRoomId()
            + " from " + booking.getCheckIn() + " to " + booking.getCheckOut()
            + " is confirmed. Total: " + booking.getTotalAmount()
        );
    }

    public void notifyCheckIn(Booking booking, Guest guest) {
        broadcast(
            guest.getEmail(),
            "Welcome! Check-in Successful: " + booking.getBookingId(),
            "Dear " + guest.getName() + ", you have checked in to room "
            + booking.getRoomId() + ". Enjoy your stay!"
        );
    }

    public void notifyCheckOut(Booking booking, Guest guest, Invoice invoice) {
        broadcast(
            guest.getEmail(),
            "Checkout Complete — Invoice: " + invoice.getInvoiceId(),
            "Dear " + guest.getName() + ", thank you for your stay. "
            + "Invoice " + invoice.getInvoiceId()
            + " for " + booking.getTotalAmount() + " has been generated."
        );
    }

    public void notifyCancellation(Booking booking, Guest guest) {
        broadcast(
            guest.getEmail(),
            "Booking Cancelled: " + booking.getBookingId(),
            "Dear " + guest.getName() + ", your booking "
            + booking.getBookingId() + " has been cancelled."
        );
    }

    private void broadcast(String recipient, String subject, String body) {
        channels.forEach(c -> c.send(recipient, subject, body));
    }
}

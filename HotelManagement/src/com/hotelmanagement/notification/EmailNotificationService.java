package com.hotelmanagement.notification;

import com.hotelmanagement.models.Booking;
import com.hotelmanagement.models.Guest;

public class EmailNotificationService implements NotificationService {
    @Override
    public void notifyBookingConfirmed(Booking booking, Guest guest) {
        System.out.println("[EMAIL] Booking confirmed for " + guest.getName() + ": " + booking.getId());
    }

    @Override
    public void notifyCheckIn(Booking booking, Guest guest) {
        System.out.println("[EMAIL] Check-in confirmed for " + guest.getName() + " | Room: " + booking.getRoomId());
    }

    @Override
    public void notifyCheckOut(Booking booking, Guest guest) {
        System.out.printf("[EMAIL] Check-out for %s | Total: ₹%.0f%n", guest.getName(), booking.getTotalAmount());
    }

    @Override
    public void notifyCancellation(Booking booking, Guest guest) {
        System.out.println("[EMAIL] Booking cancelled for " + guest.getName() + ": " + booking.getId());
    }
}

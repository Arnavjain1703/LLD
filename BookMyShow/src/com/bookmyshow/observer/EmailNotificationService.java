package com.bookmyshow.observer;

import com.bookmyshow.models.Booking;

public class EmailNotificationService implements NotificationService {
    @Override
    public void onBookingConfirmed(Booking b) {
        System.out.println("[EMAIL] Confirmed: " + b.getId() + " | " + b.getShow().getMovie().getTitle());
    }
    @Override
    public void onBookingCancelled(Booking b) {
        System.out.println("[EMAIL] Cancelled: " + b.getId());
    }
}

package com.bookmyshow.observer;

import com.bookmyshow.models.Booking;

public class SMSNotificationService implements NotificationService {
    @Override
    public void onBookingConfirmed(Booking b) {
        System.out.println("[SMS] Confirmed: " + b.getId());
    }
    @Override
    public void onBookingCancelled(Booking b) {
        System.out.println("[SMS] Cancelled: " + b.getId());
    }
}

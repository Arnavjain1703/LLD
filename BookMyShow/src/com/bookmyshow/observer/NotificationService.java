package com.bookmyshow.observer;

import com.bookmyshow.models.Booking;

public interface NotificationService {
    void onBookingConfirmed(Booking booking);
    void onBookingCancelled(Booking booking);
}

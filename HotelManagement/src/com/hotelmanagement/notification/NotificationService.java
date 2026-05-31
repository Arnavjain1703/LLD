package com.hotelmanagement.notification;

import com.hotelmanagement.models.Booking;
import com.hotelmanagement.models.Guest;

public interface NotificationService {
    void notifyBookingConfirmed(Booking booking, Guest guest);
    void notifyCheckIn(Booking booking, Guest guest);
    void notifyCheckOut(Booking booking, Guest guest);
    void notifyCancellation(Booking booking, Guest guest);
}

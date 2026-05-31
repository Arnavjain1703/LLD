package com.bookmyshow.strategy;

import com.bookmyshow.models.Booking;

public interface CancellationPolicy {
    boolean canCancel(Booking booking);
}

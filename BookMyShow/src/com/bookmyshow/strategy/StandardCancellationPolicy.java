package com.bookmyshow.strategy;

import com.bookmyshow.models.Booking;
import java.time.LocalDateTime;

public class StandardCancellationPolicy implements CancellationPolicy {
    @Override
    public boolean canCancel(Booking booking) {
        LocalDateTime cutoff = booking.getShow().getStartTime().minusHours(2);
        return LocalDateTime.now().isBefore(cutoff);
    }
}

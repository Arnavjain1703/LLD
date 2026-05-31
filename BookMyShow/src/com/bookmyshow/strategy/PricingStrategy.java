package com.bookmyshow.strategy;

import com.bookmyshow.models.Seat;
import java.util.List;

public interface PricingStrategy {
    double calculatePrice(List<Seat> seats);
}

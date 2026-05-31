package com.bookmyshow.strategy;

import com.bookmyshow.models.Seat;
import java.util.List;

public class DefaultPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(List<Seat> seats) {
        return seats.stream()
                .mapToDouble(s -> s.getBasePrice() * s.getType().getMultiplier())
                .sum();
    }
}

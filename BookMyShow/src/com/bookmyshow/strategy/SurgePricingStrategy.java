package com.bookmyshow.strategy;

import com.bookmyshow.models.Seat;
import java.util.List;

public class SurgePricingStrategy implements PricingStrategy {
    private final double surgeMultiplier;

    public SurgePricingStrategy(double surgeMultiplier) {
        this.surgeMultiplier = surgeMultiplier;
    }

    @Override
    public double calculatePrice(List<Seat> seats) {
        return seats.stream()
                .mapToDouble(s -> s.getBasePrice() * s.getType().getMultiplier() * surgeMultiplier)
                .sum();
    }
}

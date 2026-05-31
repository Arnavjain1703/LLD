package com.bookmyshow.enums;

public enum SeatType {
    REGULAR(1.0), PREMIUM(1.5), RECLINER(2.0);

    private final double multiplier;
    SeatType(double m) { this.multiplier = m; }
    public double getMultiplier() { return multiplier; }
}

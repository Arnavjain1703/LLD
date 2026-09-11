package library.fine;

import library.model.BookLending;

/** Premium members pay half the standard rate. */
public class PremiumFineStrategy implements FineStrategy {

    public static final double RATE_PER_DAY = 0.5;

    @Override
    public double calculate(BookLending lending) {
        return lending.overdueDays() * RATE_PER_DAY;
    }
}

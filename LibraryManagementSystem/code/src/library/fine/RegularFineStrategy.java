package library.fine;

import library.model.BookLending;

public class RegularFineStrategy implements FineStrategy {

    public static final double RATE_PER_DAY = 1.0;

    @Override
    public double calculate(BookLending lending) {
        return lending.overdueDays() * RATE_PER_DAY;
    }
}

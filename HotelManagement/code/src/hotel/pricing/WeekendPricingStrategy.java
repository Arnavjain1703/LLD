package hotel.pricing;

import hotel.model.Room;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Charges 1.2× the nightly rate for each night that starts on Friday or Saturday.
 * Weekday nights are charged at the base rate.
 */
public class WeekendPricingStrategy implements PricingStrategy {
    private static final double WEEKEND_MULTIPLIER = 1.2;

    @Override
    public double calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        double total = 0.0;
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            DayOfWeek day = date.getDayOfWeek();
            boolean isWeekend = (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY);
            total += isWeekend
                    ? room.getPricePerNight() * WEEKEND_MULTIPLIER
                    : room.getPricePerNight();
            date = date.plusDays(1);
        }
        return total;
    }
}

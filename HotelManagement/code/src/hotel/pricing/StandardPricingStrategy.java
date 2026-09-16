package hotel.pricing;

import hotel.model.Room;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Base strategy: nights × pricePerNight, no adjustments. */
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public double calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return nights * room.getPricePerNight();
    }
}

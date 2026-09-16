package hotel.pricing;

import hotel.model.Room;
import java.time.LocalDate;

public interface PricingStrategy {
    double calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);
}

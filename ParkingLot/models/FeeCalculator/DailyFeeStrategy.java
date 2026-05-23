package models.FeeCalculator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import enums.vechicle;
import models.Ticket;

public class DailyFeeStrategy implements FeeCalculatorInterface {
    private Map<vechicle, Double> ratePerDay;

    public DailyFeeStrategy() {
        this.ratePerDay = new HashMap<>();
        ratePerDay.put(vechicle.BIKE, 50.0);
        ratePerDay.put(vechicle.CAR, 100.0);
        ratePerDay.put(vechicle.TRUCK, 200.0);
        ratePerDay.put(vechicle.EVCAR, 120.0);
        ratePerDay.put(vechicle.WHEELCHAIR, 30.0);
    }

    @Override
    public double calculate(Ticket ticket) {
        LocalDateTime entry = ticket.getEntryTime();
        LocalDateTime exit = ticket.getExitTime() != null ? ticket.getExitTime() : LocalDateTime.now();
        long days = Duration.between(entry, exit).toDays() + 1; // round up
        double rate = ratePerDay.getOrDefault(ticket.getVehicle().getVehicalType(), 100.0);
        return days * rate;
    }
}

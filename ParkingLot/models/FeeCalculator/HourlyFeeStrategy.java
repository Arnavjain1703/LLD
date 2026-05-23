package models.FeeCalculator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import enums.vechicle;
import models.Ticket;

public class HourlyFeeStrategy implements FeeCalculatorInterface {
    private Map<vechicle, Double> ratePerHour;

    public HourlyFeeStrategy() {
        this.ratePerHour = new HashMap<>();
        ratePerHour.put(vechicle.BIKE, 10.0);
        ratePerHour.put(vechicle.CAR, 20.0);
        ratePerHour.put(vechicle.TRUCK, 40.0);
        ratePerHour.put(vechicle.EVCAR, 25.0);
        ratePerHour.put(vechicle.WHEELCHAIR, 5.0);
    }

    @Override
    public double calculate(Ticket ticket) {
        LocalDateTime entry = ticket.getEntryTime();
        LocalDateTime exit = ticket.getExitTime() != null ? ticket.getExitTime() : LocalDateTime.now();
        long hours = Duration.between(entry, exit).toHours() + 1; // round up
        double rate = ratePerHour.getOrDefault(ticket.getVehicle().getVehicalType(), 20.0);
        return hours * rate;
    }
}

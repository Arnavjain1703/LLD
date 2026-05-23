package models.parkingSpot;

import enums.parkingSpot;
import enums.vechicle;

public class LargeSpot extends AbstractParkingSpot {
    public LargeSpot() {
        super(parkingSpot.LARGESPOT);
    }

    @Override
    public boolean canFitVehicle(vechicle vehicleType) {
        // Large spots can fit: Car, Truck, Bike (anything except EV-only and Wheelchair)
        return vehicleType == vechicle.CAR
                || vehicleType == vechicle.TRUCK
                || vehicleType == vechicle.BIKE;
    }
}

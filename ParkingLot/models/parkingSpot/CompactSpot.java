package models.parkingSpot;

import enums.parkingSpot;
import enums.vechicle;

public class CompactSpot extends AbstractParkingSpot {
    public CompactSpot() {
        super(parkingSpot.COMPACTSPOT);
    }

    @Override
    public boolean canFitVehicle(vechicle vehicleType) {
        // Compact spots can fit: Car, Bike
        return vehicleType == vechicle.CAR || vehicleType == vechicle.BIKE;
    }
}

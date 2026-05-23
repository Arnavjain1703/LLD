package models.parkingSpot;

import enums.parkingSpot;
import enums.vechicle;

public class EvSpot extends AbstractParkingSpot {
    public EvSpot() {
        super(parkingSpot.EVSPOT);
    }

    @Override
    public boolean canFitVehicle(vechicle vehicleType) {
        // EV spots are exclusively for EV cars
        return vehicleType == vechicle.EVCAR;
    }
}

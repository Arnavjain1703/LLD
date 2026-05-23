package models.parkingSpot;

import enums.parkingSpot;
import enums.vechicle;

public class HandiCappedSpot extends AbstractParkingSpot {
    public HandiCappedSpot() {
        super(parkingSpot.HANDICAPPEDSPOT);
    }

    @Override
    public boolean canFitVehicle(vechicle vehicleType) {
        // Handicapped spots are for wheelchair vehicles only
        return vehicleType == vechicle.WHEELCHAIR;
    }
}

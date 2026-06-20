package models.SpotAssignmentStrategy;

import java.util.List;

import models.ParkingFloor;
import models.Vehical;
import models.parkingSpot.AbstractParkingSpot;

/**
 * O(log n) per floor — uses TreeSet.first() to get the nearest available spot.
 * Iterates floors from first (nearest entry) to last.
 */
public class NearestSpotStrategy implements SpotAssignmentStrategyInterface {

    @Override
    public AbstractParkingSpot findParkingSpot(List<ParkingFloor> parkingFloors, Vehical vehical) {
        for (ParkingFloor floor : parkingFloors) {
            AbstractParkingSpot spot = floor.findAndParkNearest(vehical.getVehicalType());
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }
}

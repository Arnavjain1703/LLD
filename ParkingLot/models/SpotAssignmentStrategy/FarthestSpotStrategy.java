package models.SpotAssignmentStrategy;

import java.util.List;

import models.ParkingFloor;
import models.Vehical;
import models.parkingSpot.AbstractParkingSpot;

/**
 * O(log n) per floor — uses TreeSet.last() to get the farthest available spot.
 * Iterates floors from last (farthest from entry) to first.
 */
public class FarthestSpotStrategy implements SpotAssignmentStrategyInterface {

    @Override
    public AbstractParkingSpot findParkingSpot(List<ParkingFloor> parkingFloors, Vehical vehical) {
        for (int i = parkingFloors.size() - 1; i >= 0; i--) {
            ParkingFloor floor = parkingFloors.get(i);
            AbstractParkingSpot spot = floor.getFarthestAvailableSpot(vehical.getVehicalType());
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }
}

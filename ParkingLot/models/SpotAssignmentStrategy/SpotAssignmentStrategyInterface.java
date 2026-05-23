package models.SpotAssignmentStrategy;

import java.util.List;

import models.ParkingFloor;
import models.Vehical;
import models.parkingSpot.AbstractParkingSpot;

public interface SpotAssignmentStrategyInterface {
    AbstractParkingSpot findParkingSpot(List<ParkingFloor> parkingFloors, Vehical vehical);
}

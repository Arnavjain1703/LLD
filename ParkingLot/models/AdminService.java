package models;

import models.FeeCalculator.FeeCalculatorInterface;
import models.parkingSpot.AbstractParkingSpot;

public class AdminService {
    private ParkingLot parkingLot;

    public AdminService(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    public void addFloor(ParkingFloor floor) {
        parkingLot.addFloor(floor);
    }

    public void removeFloor(int floorNumber) {
        parkingLot.removeFloor(floorNumber);
    }

    public void addSpot(int floorNumber, AbstractParkingSpot spot) {
        for (ParkingFloor floor : parkingLot.getFloors()) {
            if (floor.getFloorNumber() == floorNumber) {
                floor.addSpot(spot);
                return;
            }
        }
        throw new RuntimeException("Floor " + floorNumber + " not found");
    }

    public void addEntryGate(EntryGate gate) {
        parkingLot.addEntryGate(gate);
    }

    public void removeEntryGate(String gateId) {
        parkingLot.removeEntryGate(gateId);
    }

    public void addExitGate(ExitGate gate) {
        parkingLot.addExitGate(gate);
    }

    public void removeExitGate(String gateId) {
        parkingLot.removeExitGate(gateId);
    }
}

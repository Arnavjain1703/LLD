package models;

import java.util.List;

import models.SpotAssignmentStrategy.SpotAssignmentStrategyInterface;
import models.parkingSpot.AbstractParkingSpot;

public class EntryGate {
    private String gateId;
    private SpotAssignmentStrategyInterface assignmentStrategy;

    public EntryGate(String gateId, SpotAssignmentStrategyInterface assignmentStrategy) {
        this.gateId = gateId;
        this.assignmentStrategy = assignmentStrategy;
    }

    public Ticket processEntry(Vehical vehicle, List<ParkingFloor> parkingFloors) {
        AbstractParkingSpot spot = assignmentStrategy.findParkingSpot(parkingFloors, vehicle);

        if (spot == null) {
            throw new RuntimeException("No spot available for vehicle: " + vehicle.getVehicalType());
        }

        ParkingFloor floor = findFloorForSpot(spot, parkingFloors);
        spot.park(vehicle);
        Ticket ticket = new Ticket(vehicle, spot, floor);
        System.out.println("Gate " + gateId + " issued ticket " + ticket.getTicketId());
        return ticket;
    }

    private ParkingFloor findFloorForSpot(AbstractParkingSpot spot, List<ParkingFloor> floors) {
        int floorNumber = spot.getSpotId() / 1000;
        for (ParkingFloor floor : floors) {
            if (floor.getFloorNumber() == floorNumber) {
                return floor;
            }
        }
        return floors.get(0);
    }

    public String getGateId() {
        return gateId;
    }
}

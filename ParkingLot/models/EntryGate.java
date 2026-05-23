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

    /**
     * Processes vehicle entry:
     * 1. Finds an available spot using the assignment strategy
     * 2. Parks the vehicle in the spot
     * 3. Creates and returns a Ticket
     */
    public Ticket processEntry(Vehical vehicle, List<ParkingFloor> parkingFloors) {
        AbstractParkingSpot spot = assignmentStrategy.findParkingSpot(parkingFloors, vehicle);

        if (spot == null) {
            throw new RuntimeException("No spot available for vehicle: " + vehicle.getVehicalType());
        }

        spot.park(vehicle);
        Ticket ticket = new Ticket(vehicle, spot);
        System.out.println("Gate " + gateId + " issued ticket " + ticket.getTicketId());
        return ticket;
    }

    public String getGateId() {
        return gateId;
    }
}

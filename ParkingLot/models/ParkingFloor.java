package models;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import enums.parkingSpotState;
import enums.vechicle;
import models.parkingSpot.AbstractParkingSpot;

public class ParkingFloor {
    private int floorNumber;
    private List<AbstractParkingSpot> spots;

    // Single TreeSet of ALL available spots, sorted by spotId
    private TreeSet<AbstractParkingSpot> availableSpots;

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
        this.availableSpots = new TreeSet<>();
        System.out.println("Created new floor with floorNumber " + floorNumber);
    }

    // ===================== ADD / REMOVE =====================

    public synchronized void addSpot(AbstractParkingSpot spot) {
        spot.setSpotId(floorNumber * 1000 + spots.size());
        spots.add(spot);
        availableSpots.add(spot);
        System.out.println("Added " + spot.getSpotType() + " spot to floor " + floorNumber);
    }

    public synchronized void removeSpot(AbstractParkingSpot spot) {
        spots.remove(spot);
        availableSpots.remove(spot);
        System.out.println("Removed spot from floor " + floorNumber);
    }

    // ===================== FIND (used by strategies) =====================

    /**
     * Finds the nearest available spot for the given vehicle type.
     * Used by NearestSpotStrategy.
     */
    public synchronized AbstractParkingSpot getNearestAvailableSpot(vechicle vehicleType) {
        for (AbstractParkingSpot spot : availableSpots) {
            if (spot.canFitVehicle(vehicleType)) {
                return spot;
            }
        }
        return null;
    }

    /**
     * Finds the farthest available spot for the given vehicle type.
     * Used by FarthestSpotStrategy.
     */
    public synchronized AbstractParkingSpot getFarthestAvailableSpot(vechicle vehicleType) {
        for (AbstractParkingSpot spot : availableSpots.descendingSet()) {
            if (spot.canFitVehicle(vehicleType)) {
                return spot;
            }
        }
        return null;
    }

    // ===================== FIND + PARK (ATOMIC) =====================

    /**
     * ATOMIC: Finds nearest spot AND removes from available pool in one lock.
     * Prevents two threads from getting the same spot.
     */
    public synchronized AbstractParkingSpot findAndParkNearest(vechicle vehicleType) {
        for (AbstractParkingSpot spot : availableSpots) {
            if (spot.canFitVehicle(vehicleType)) {
                availableSpots.remove(spot);
                return spot;
            }
        }
        return null;
    }

    /**
     * ATOMIC: Finds farthest spot AND removes from available pool in one lock.
     */
    public synchronized AbstractParkingSpot findAndParkFarthest(vechicle vehicleType) {
        for (AbstractParkingSpot spot : availableSpots.descendingSet()) {
            if (spot.canFitVehicle(vehicleType)) {
                availableSpots.remove(spot);
                return spot;
            }
        }
        return null;
    }

    // ===================== PARK / UNPARK =====================

    public synchronized void parkVehicle(AbstractParkingSpot spot) {
        availableSpots.remove(spot);
    }

    public synchronized void unparkVehicle(AbstractParkingSpot spot) {
        availableSpots.add(spot);
    }

    // ===================== GETTERS =====================

    public List<AbstractParkingSpot> getSpots() {
        return spots;
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}

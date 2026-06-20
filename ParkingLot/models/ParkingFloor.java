package models;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import enums.vechicle;
import models.parkingSpot.AbstractParkingSpot;

public class ParkingFloor {
    private int floorNumber;
    private List<AbstractParkingSpot> spots;
    private TreeSet<AbstractParkingSpot> availableSpots;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
        this.availableSpots = new TreeSet<>();
        System.out.println("Created new floor with floorNumber " + floorNumber);
    }

    // ===================== ADD / REMOVE (WRITE) =====================

    public void addSpot(AbstractParkingSpot spot) {
        writeLock.lock();
        try {
            spot.setSpotId(floorNumber * 1000 + spots.size());
            spots.add(spot);
            availableSpots.add(spot);
            System.out.println("Added " + spot.getSpotType() + " spot to floor " + floorNumber);
        } finally {
            writeLock.unlock();
        }
    }

    public void removeSpot(AbstractParkingSpot spot) {
        writeLock.lock();
        try {
            spots.remove(spot);
            availableSpots.remove(spot);
            System.out.println("Removed spot from floor " + floorNumber);
        } finally {
            writeLock.unlock();
        }
    }

    // ===================== FIND (READ-ONLY) =====================

    public AbstractParkingSpot getNearestAvailableSpot(vechicle vehicleType) {
        readLock.lock();
        try {
            for (AbstractParkingSpot spot : availableSpots) {
                if (spot.canFitVehicle(vehicleType)) {
                    return spot;
                }
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    // ===================== FIND + PARK (WRITE — atomic) =====================

    public AbstractParkingSpot findAndParkNearest(vechicle vehicleType) {
        writeLock.lock();
        try {
            for (AbstractParkingSpot spot : availableSpots) {
                if (spot.canFitVehicle(vehicleType)) {
                    availableSpots.remove(spot);
                    return spot;
                }
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    public AbstractParkingSpot findAndParkFarthest(vechicle vehicleType) {
        writeLock.lock();
        try {
            for (AbstractParkingSpot spot : availableSpots.descendingSet()) {
                if (spot.canFitVehicle(vehicleType)) {
                    availableSpots.remove(spot);
                    return spot;
                }
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    // ===================== UNPARK (WRITE) =====================

    public void unparkVehicle(AbstractParkingSpot spot) {
        writeLock.lock();
        try {
            availableSpots.add(spot);
        } finally {
            writeLock.unlock();
        }
    }

    // ===================== GETTERS =====================

    public List<AbstractParkingSpot> getSpots() {
        readLock.lock();
        try {
            return new ArrayList<>(spots);
        } finally {
            readLock.unlock();
        }
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}

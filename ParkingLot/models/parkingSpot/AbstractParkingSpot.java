package models.parkingSpot;

import enums.parkingSpot;
import enums.parkingSpotState;
import enums.vechicle;
import models.Vehical;


public abstract class AbstractParkingSpot implements Comparable<AbstractParkingSpot> {
    private parkingSpot spotType;
    private parkingSpotState state;
    private int spotId; // unique position: (floorNumber * 1000) + spotIndex
    private Vehical vehical;

    protected AbstractParkingSpot(parkingSpot spotType) {
        this.spotType = spotType;
        this.state = parkingSpotState.AVAILABLE;
    }

    public parkingSpot getSpotType() {
        return spotType;
    }

    public parkingSpotState getState() {
        return state;
    }

    public int getSpotId() {
        return spotId;
    }

    public void setSpotId(int spotId) {
        this.spotId = spotId;
    }

    protected void setState(parkingSpotState state) {
        this.state = state;
        System.out.println("Updated the state of spot to " + state);
    }

    public synchronized void park(Vehical vehical) {
        if (this.state == parkingSpotState.OCCUPIED) {
            throw new IllegalStateException("Spot " + spotId + " is already occupied");
        }
        setState(parkingSpotState.OCCUPIED);
        this.vehical = vehical;
    }

    public synchronized void unpark() {
        if (this.state == parkingSpotState.AVAILABLE) {
            throw new IllegalStateException("Spot " + spotId + " is already empty");
        }
        setState(parkingSpotState.AVAILABLE);
        this.vehical = null;
    }

    /**
     * Each spot subclass defines which vehicle types it can accommodate.
     */
    public abstract boolean canFitVehicle(vechicle vehicleType);

    /**
     * Natural ordering by spotId — lower spotId = nearer to entry.
     * This enables TreeSet to give us nearest (first()) and farthest (last()).
     */
    @Override
    public int compareTo(AbstractParkingSpot other) {
        return Integer.compare(this.spotId, other.spotId);
    }
}

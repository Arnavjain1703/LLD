package models;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import enums.TicketStatus;
import models.parkingSpot.AbstractParkingSpot;

public class Ticket {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private String ticketId;
    private Vehical vehicle;
    private AbstractParkingSpot spot;
    private ParkingFloor floor;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;

    public Ticket(Vehical vehicle, AbstractParkingSpot spot, ParkingFloor floor) {
        this.ticketId = "TKT-" + counter.incrementAndGet();
        this.vehicle = vehicle;
        this.spot = spot;
        this.floor = floor;
        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehical getVehicle() {
        return vehicle;
    }

    public AbstractParkingSpot getSpot() {
        return spot;
    }

    public ParkingFloor getFloor() {
        return floor;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}

package models;

import java.time.LocalDateTime;

import enums.TicketStatus;
import models.parkingSpot.AbstractParkingSpot;

public class Ticket {
    private static int counter = 0;
    private String ticketId;
    private Vehical vehicle;
    private AbstractParkingSpot spot;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;

    public Ticket(Vehical vehicle, AbstractParkingSpot spot) {
        this.ticketId = "TKT-" + (++counter);
        this.vehicle = vehicle;
        this.spot = spot;
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

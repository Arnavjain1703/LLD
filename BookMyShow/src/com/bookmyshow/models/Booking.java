package com.bookmyshow.models;

import com.bookmyshow.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public class Booking {
    private final String        id;
    private final String        userId;
    private final Show          show;
    private final List<Seat>    seats;
    private final double        totalPrice;
    private final LocalDateTime createdAt;
    private volatile BookingStatus status;
    private volatile String     paymentId;

    public Booking(String id, String userId, Show show, List<Seat> seats, double totalPrice) {
        this.id         = id;
        this.userId     = userId;
        this.show       = show;
        this.seats      = List.copyOf(seats);
        this.totalPrice = totalPrice;
        this.status     = BookingStatus.PENDING;
        this.createdAt  = LocalDateTime.now();
    }

    public synchronized void confirm(String paymentId) {
        this.status    = BookingStatus.CONFIRMED;
        this.paymentId = paymentId;
    }

    public synchronized void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public String        getId()         { return id; }
    public String        getUserId()     { return userId; }
    public Show          getShow()       { return show; }
    public List<Seat>    getSeats()      { return seats; }
    public double        getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus()     { return status; }
    public String        getPaymentId()  { return paymentId; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}

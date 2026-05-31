package com.hotelmanagement.models;

import com.hotelmanagement.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking {
    private final String id;
    private final String guestId;
    private final String roomId;
    private final String hotelId;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private volatile BookingStatus status;
    private double totalAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime holdExpiryTime;

    public Booking(String id, String guestId, String roomId, String hotelId,
                   LocalDate checkInDate, LocalDate checkOutDate) {
        this.id = id;
        this.guestId = guestId;
        this.roomId = roomId;
        this.hotelId = hotelId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = BookingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.holdExpiryTime = createdAt.plusMinutes(15);
    }

    public long getNights() { return checkInDate.until(checkOutDate).getDays(); }
    public boolean isHoldExpired() { return LocalDateTime.now().isAfter(holdExpiryTime); }

    public String getId() { return id; }
    public String getGuestId() { return guestId; }
    public String getRoomId() { return roomId; }
    public String getHotelId() { return hotelId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getHoldExpiryTime() { return holdExpiryTime; }

    @Override
    public String toString() {
        return String.format("Booking[%s | Room:%s | %s to %s | %s | ₹%.0f]",
                id, roomId, checkInDate, checkOutDate, status, totalAmount);
    }
}

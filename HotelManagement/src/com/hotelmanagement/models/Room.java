package com.hotelmanagement.models;

import com.hotelmanagement.enums.RoomStatus;
import com.hotelmanagement.enums.RoomType;
import java.util.concurrent.locks.ReentrantLock;

public class Room implements Comparable<Room> {
    private final String id;
    private final String hotelId;
    private final int floorNumber;
    private final int roomNumber;
    private final RoomType type;
    private volatile RoomStatus status;
    private double basePricePerNight;
    private final ReentrantLock lock = new ReentrantLock();

    public Room(String id, String hotelId, int floorNumber, int roomNumber,
                RoomType type, double basePricePerNight) {
        this.id = id;
        this.hotelId = hotelId;
        this.floorNumber = floorNumber;
        this.roomNumber = roomNumber;
        this.type = type;
        this.basePricePerNight = basePricePerNight;
        this.status = RoomStatus.AVAILABLE;
    }

    public boolean acquireLock() { return lock.tryLock(); }
    public void releaseLock() { lock.unlock(); }
    public void lockInterruptibly() throws InterruptedException { lock.lockInterruptibly(); }

    // Comparable by price for TreeSet ordering
    @Override
    public int compareTo(Room other) {
        int cmp = Double.compare(this.basePricePerNight, other.basePricePerNight);
        return cmp != 0 ? cmp : this.id.compareTo(other.id);
    }

    public String getId() { return id; }
    public String getHotelId() { return hotelId; }
    public int getFloorNumber() { return floorNumber; }
    public int getRoomNumber() { return roomNumber; }
    public RoomType getType() { return type; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public double getBasePricePerNight() { return basePricePerNight; }
    public void setBasePricePerNight(double price) { this.basePricePerNight = price; }

    @Override
    public String toString() {
        return String.format("Room[%s | Floor %d | #%d | %s | ₹%.0f/night | %s]",
                id, floorNumber, roomNumber, type, basePricePerNight, status);
    }
}

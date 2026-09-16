package hotel.model;

import hotel.exception.RoomNotAvailableException;

public class Room {
    private final String roomId;
    private final String hotelId;
    private final int roomNumber;
    private final RoomType type;
    private final double pricePerNight;
    private RoomStatus status;

    public Room(String roomId, String hotelId, int roomNumber, RoomType type, double pricePerNight) {
        this.roomId       = roomId;
        this.hotelId      = hotelId;
        this.roomNumber   = roomNumber;
        this.type         = type;
        this.pricePerNight = pricePerNight;
        this.status       = RoomStatus.AVAILABLE;
    }

    public String getRoomId()        { return roomId; }
    public String getHotelId()       { return hotelId; }
    public int getRoomNumber()       { return roomNumber; }
    public RoomType getType()        { return type; }
    public double getPricePerNight() { return pricePerNight; }
    public RoomStatus getStatus()    { return status; }

    public void book() {
        if (status != RoomStatus.AVAILABLE)
            throw new RoomNotAvailableException("Room " + roomNumber + " is not AVAILABLE (status=" + status + ")");
        status = RoomStatus.BOOKED;
    }

    public void checkIn() {
        if (status != RoomStatus.BOOKED)
            throw new RoomNotAvailableException("Room " + roomNumber + " is not BOOKED (status=" + status + ")");
        status = RoomStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != RoomStatus.CHECKED_IN)
            throw new RoomNotAvailableException("Room " + roomNumber + " is not CHECKED_IN (status=" + status + ")");
        status = RoomStatus.AVAILABLE;
    }

    public void markMaintenance() {
        status = RoomStatus.MAINTENANCE;
    }

    public void clearMaintenance() {
        if (status != RoomStatus.MAINTENANCE)
            throw new IllegalStateException("Room " + roomNumber + " is not under MAINTENANCE");
        status = RoomStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Room{id='" + roomId + "', number=" + roomNumber
                + ", type=" + type + ", price=" + pricePerNight + ", status=" + status + "}";
    }
}

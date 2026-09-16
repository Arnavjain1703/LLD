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
        this.roomId = roomId;
        this.hotelId = hotelId;
        this.roomNumber = roomNumber;
        this.type = type;
        this.pricePerNight = pricePerNight;
        this.status = RoomStatus.AVAILABLE;
    }

    public String getRoomId()         { return roomId; }
    public String getHotelId()        { return hotelId; }
    public int getRoomNumber()        { return roomNumber; }
    public RoomType getType()         { return type; }
    public double getPricePerNight()  { return pricePerNight; }
    public RoomStatus getStatus()     { return status; }

    /** Called when a booking is confirmed — room moves to BOOKED. */
    public void book() {
        if (status != RoomStatus.AVAILABLE) {
            throw new RoomNotAvailableException("Room " + roomNumber + " is not available (status=" + status + ")");
        }
        status = RoomStatus.BOOKED;
    }

    /** Called at guest check-in. */
    public void checkIn() {
        if (status != RoomStatus.BOOKED) {
            throw new RoomNotAvailableException("Room " + roomNumber + " is not booked (status=" + status + ")");
        }
        status = RoomStatus.CHECKED_IN;
    }

    /** Called at guest check-out — room returns to AVAILABLE. */
    public void checkOut() {
        if (status != RoomStatus.CHECKED_IN) {
            throw new RoomNotAvailableException("Room " + roomNumber + " is not checked-in (status=" + status + ")");
        }
        status = RoomStatus.AVAILABLE;
    }

    /** Flag for housekeeping / repair. */
    public void markMaintenance() {
        status = RoomStatus.MAINTENANCE;
    }

    /** Clear maintenance flag — room is ready again. */
    public void clearMaintenance() {
        if (status != RoomStatus.MAINTENANCE) {
            throw new IllegalStateException("Room " + roomNumber + " is not under maintenance");
        }
        status = RoomStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Room{id='" + roomId + "', number=" + roomNumber + ", type=" + type + ", price=" + pricePerNight + ", status=" + status + "}";
    }
}

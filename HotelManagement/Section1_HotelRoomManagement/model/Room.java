package model;

import java.math.BigDecimal;

/**
 * Room is a physical unit — identified by roomId, typed by RoomType.
 *
 * Does NOT hold hotelId. The hotel-room association is owned by
 * RoomRepository as Map<hotelId, Map<roomId, Room>>.
 * Room is pure data about the physical unit itself.
 */
public class Room {
    private final String     roomId;
    private final String     roomNumber;
    private final int        floor;
    private final RoomType   type;
    private final int        capacity;
    private final BigDecimal pricePerNight;
    private       RoomStatus status;

    public Room(String roomId, String roomNumber,
                int floor, RoomType type, int capacity, BigDecimal pricePerNight) {
        this.roomId        = roomId;
        this.roomNumber    = roomNumber;
        this.floor         = floor;
        this.type          = type;
        this.capacity      = capacity;
        this.pricePerNight = pricePerNight;
        this.status        = RoomStatus.AVAILABLE;
    }

    public String     getRoomId()        { return roomId; }
    public String     getRoomNumber()    { return roomNumber; }
    public int        getFloor()         { return floor; }
    public RoomType   getType()          { return type; }
    public int        getCapacity()      { return capacity; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public RoomStatus getStatus()        { return status; }

    public void setStatus(RoomStatus status) { this.status = status; }

    public boolean isAvailable() {
        return this.status == RoomStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " [" + type + "] - " + status
             + " @ $" + pricePerNight + "/night";
    }
}

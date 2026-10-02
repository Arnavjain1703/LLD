package model;

import java.math.BigDecimal;

/**
 * Room is a physical unit (like a book copy in a library).
 * It references its hotel via hotelId - no back-pointer to Hotel object.
 *
 * RoomType  = the category (SINGLE, DOUBLE, SUITE, PENTHOUSE)
 * RoomStatus = current state (AVAILABLE, BOOKED, OCCUPIED, UNDER_MAINTENANCE)
 */
public class Room {
    private final String     roomId;
    private final String     hotelId;        // FK to Hotel - Room belongs to exactly one Hotel
    private final String     roomNumber;
    private final int        floor;
    private final RoomType   type;
    private final int        capacity;
    private final BigDecimal pricePerNight;
    private       RoomStatus status;         // mutable - changes with bookings

    public Room(String roomId, String hotelId, String roomNumber,
                int floor, RoomType type, int capacity, BigDecimal pricePerNight) {
        this.roomId        = roomId;
        this.hotelId       = hotelId;
        this.roomNumber    = roomNumber;
        this.floor         = floor;
        this.type          = type;
        this.capacity      = capacity;
        this.pricePerNight = pricePerNight;
        this.status        = RoomStatus.AVAILABLE; // default on creation
    }

    public String     getRoomId()        { return roomId; }
    public String     getHotelId()       { return hotelId; }
    public String     getRoomNumber()    { return roomNumber; }
    public int        getFloor()         { return floor; }
    public RoomType   getType()          { return type; }
    public int        getCapacity()      { return capacity; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public RoomStatus getStatus()        { return status; }

    // Only RoomService / BookingService should call this
    public void setStatus(RoomStatus status) { this.status = status; }

    public boolean isAvailable() {
        return this.status == RoomStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " [" + type + "] - " + status
             + " @ $" + pricePerNight + "/night (capacity: " + capacity + ")";
    }
}

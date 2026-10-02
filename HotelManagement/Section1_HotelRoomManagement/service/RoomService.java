package service;

import model.Room;
import model.RoomStatus;
import repository.HotelRepository;
import repository.RoomRepository;

import java.util.List;

/**
 * RoomService - room lifecycle ONLY.
 *
 * Responsibility: add, remove, fetch rooms; manage room status.
 * Actor: Hotel Admin.
 *
 * Dependencies: RoomRepository + HotelRepository (to validate hotel exists before adding).
 * Does NOT know about HotelService - zero coupling between the two services.
 */
public class RoomService {

    private final RoomRepository  roomRepository;
    private final HotelRepository hotelRepository; // validates hotel exists before addRoom

    public RoomService(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository  = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    /** Add a room to an existing hotel. Guards that hotel exists first. */
    public void addRoom(Room room) {
        if (!hotelRepository.exists(room.getHotelId())) {
            throw new IllegalArgumentException(
                "Cannot add room - hotel not found: " + room.getHotelId());
        }
        roomRepository.save(room);
        System.out.println("Added to hotel " + room.getHotelId() + ": " + room);
    }

    public void removeRoom(String roomId) {
        getRoom(roomId); // guard: must exist
        roomRepository.delete(roomId);
        System.out.println("Removed room: " + roomId);
    }

    /** Remove all rooms for a hotel - called by facade before deregisterHotel. */
    public void removeAllRoomsForHotel(String hotelId) {
        roomRepository.findByHotelId(hotelId)
            .forEach(r -> roomRepository.delete(r.getRoomId()));
        System.out.println("Removed all rooms for hotel: " + hotelId);
    }

    /** Direct ID lookup - used internally by BookingService and SearchService. */
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    /** Admin view - ALL rooms in a hotel regardless of status. */
    public List<Room> getRoomsByHotel(String hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    /** Blocks a room from being booked. Cannot mark an OCCUPIED room. */
    public void markUnderMaintenance(String roomId) {
        Room room = getRoom(roomId);
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException(
                "Cannot mark occupied room for maintenance: " + roomId);
        }
        room.setStatus(RoomStatus.UNDER_MAINTENANCE);
        System.out.println("Room marked for maintenance: " + roomId);
    }

    /** Restores a room to available after maintenance completes. */
    public void markAvailable(String roomId) {
        Room room = getRoom(roomId);
        if (room.getStatus() != RoomStatus.UNDER_MAINTENANCE) {
            throw new IllegalStateException("Room is not under maintenance: " + roomId);
        }
        room.setStatus(RoomStatus.AVAILABLE);
        System.out.println("Room restored to available: " + roomId);
    }
}

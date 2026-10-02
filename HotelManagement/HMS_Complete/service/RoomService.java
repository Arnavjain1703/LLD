package service;

import model.Room;
import model.RoomStatus;
import repository.HotelRepository;
import repository.RoomRepository;

import java.util.List;

/**
 * RoomService — room lifecycle ONLY.
 *
 * Responsibility: add, remove, fetch rooms; manage room status (maintenance).
 * Actor: Hotel Admin.
 *
 * Does NOT handle hotel registration — that is HotelService.
 * Does NOT answer availability questions — that is SearchService.
 */
public class RoomService {

    private final RoomRepository  roomRepository;
    private final HotelRepository hotelRepository; // to validate hotel exists before adding a room

    public RoomService(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository  = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    /**
     * Add a room to an existing hotel.
     * hotelId is passed separately — Room has no hotelId field;
     * the association is owned by RoomRepository.
     */
    public void addRoom(String hotelId, Room room) {
        if (!hotelRepository.exists(hotelId)) {
            throw new IllegalArgumentException("Cannot add room — hotel not found: " + hotelId);
        }
        roomRepository.save(hotelId, room);
        System.out.println("Added to hotel " + hotelId + ": " + room);
    }

    public void removeRoom(String roomId) {
        getRoom(roomId); // guard: must exist
        roomRepository.delete(roomId);
        System.out.println("Removed room: " + roomId);
    }

    /** Remove all rooms belonging to a hotel — called by facade on deregister. */
    public void removeAllRoomsForHotel(String hotelId) {
        roomRepository.deleteAllByHotelId(hotelId);
        System.out.println("Removed all rooms for hotel: " + hotelId);
    }

    /** Direct ID lookup — used internally by BookingService and SearchService. */
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    /** Admin view — ALL rooms in a hotel regardless of status. */
    public List<Room> getRoomsByHotel(String hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    /** Mark a room for maintenance — blocks it from being booked. */
    public void markUnderMaintenance(String roomId) {
        Room room = getRoom(roomId);
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot mark occupied room for maintenance: " + roomId);
        }
        room.setStatus(RoomStatus.UNDER_MAINTENANCE);
        System.out.println("Room marked for maintenance: " + roomId);
    }

    /** Restore a room to available after maintenance completes. */
    public void markAvailable(String roomId) {
        Room room = getRoom(roomId);
        if (room.getStatus() != RoomStatus.UNDER_MAINTENANCE) {
            throw new IllegalStateException("Room is not under maintenance: " + roomId);
        }
        room.setStatus(RoomStatus.AVAILABLE);
        System.out.println("Room restored to available: " + roomId);
    }
}

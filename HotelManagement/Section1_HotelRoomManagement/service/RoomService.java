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
 * addRoom(hotelId, room) - hotelId passed separately because Room no longer
 * holds it. The repository owns the hotel-room association.
 */
public class RoomService {

    private final RoomRepository  roomRepository;
    private final HotelRepository hotelRepository;

    public RoomService(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository  = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    /** Add a room to a hotel. hotelId passed separately — Room does not hold it. */
    public void addRoom(String hotelId, Room room) {
        if (!hotelRepository.exists(hotelId)) {
            throw new IllegalArgumentException(
                "Cannot add room - hotel not found: " + hotelId);
        }
        roomRepository.save(hotelId, room);
        System.out.println("Added to hotel " + hotelId + ": " + room);
    }

    public void removeRoom(String roomId) {
        getRoom(roomId); // guard: must exist
        roomRepository.delete(roomId);
        System.out.println("Removed room: " + roomId);
    }

    /** Remove all rooms for a hotel — O(1) via nested map. Called by facade on deregister. */
    public void removeAllRoomsForHotel(String hotelId) {
        roomRepository.deleteAllByHotelId(hotelId);
        System.out.println("Removed all rooms for hotel: " + hotelId);
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    public List<Room> getRoomsByHotel(String hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    public void markUnderMaintenance(String roomId) {
        Room room = getRoom(roomId);
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException(
                "Cannot mark occupied room for maintenance: " + roomId);
        }
        room.setStatus(RoomStatus.UNDER_MAINTENANCE);
        System.out.println("Room marked for maintenance: " + roomId);
    }

    public void markAvailable(String roomId) {
        Room room = getRoom(roomId);
        if (room.getStatus() != RoomStatus.UNDER_MAINTENANCE) {
            throw new IllegalStateException("Room is not under maintenance: " + roomId);
        }
        room.setStatus(RoomStatus.AVAILABLE);
        System.out.println("Room restored to available: " + roomId);
    }
}

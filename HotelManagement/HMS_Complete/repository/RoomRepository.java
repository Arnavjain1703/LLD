package repository;

import model.Room;
import model.RoomStatus;
import model.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RoomRepository owns the hotel-room association.
 *
 * Primary store : Map<hotelId, Map<roomId, Room>>
 *   - findByHotelId  = O(1) direct map lookup
 *   - removeAllForHotel = O(1) single map.remove()
 *
 * Secondary index: Map<roomId, hotelId>
 *   - findById       = O(1) via index then primary lookup
 *   - delete         = O(1) via index
 *
 * Why not Map<roomId, Room> with hotelId in Room?
 *   The association belongs in the repository, not in the entity.
 *   Room is a pure value object describing a physical unit.
 */
public class RoomRepository {

    // Primary store: hotelId -> (roomId -> Room)
    private final Map<String, Map<String, Room>> store = new ConcurrentHashMap<>();

    // Secondary index: roomId -> hotelId  (for O(1) direct room lookup)
    private final Map<String, String> roomHotelIndex = new ConcurrentHashMap<>();

    public void save(String hotelId, Room room) {
        store.computeIfAbsent(hotelId, id -> new ConcurrentHashMap<>())
             .put(room.getRoomId(), room);
        roomHotelIndex.put(room.getRoomId(), hotelId);
    }

    /** Direct room lookup via secondary index — O(1). */
    public Optional<Room> findById(String roomId) {
        String hotelId = roomHotelIndex.get(roomId);
        if (hotelId == null) return Optional.empty();
        Map<String, Room> hotelRooms = store.get(hotelId);
        if (hotelRooms == null) return Optional.empty();
        return Optional.ofNullable(hotelRooms.get(roomId));
    }

    /** Which hotel does this room belong to? */
    public Optional<String> findHotelIdByRoomId(String roomId) {
        return Optional.ofNullable(roomHotelIndex.get(roomId));
    }

    /** All rooms for a hotel — O(1). */
    public List<Room> findByHotelId(String hotelId) {
        Map<String, Room> hotelRooms = store.get(hotelId);
        if (hotelRooms == null) return new ArrayList<>();
        return new ArrayList<>(hotelRooms.values());
    }

    /** Available rooms in a hotel filtered by type — used by SearchService. */
    public List<Room> findAvailableByHotelIdAndType(String hotelId, RoomType type) {
        return findByHotelId(hotelId).stream()
            .filter(r -> type == null || r.getType() == type)
            .filter(Room::isAvailable)
            .collect(Collectors.toList());
    }

    /** Rooms by status across the chain — used by admin ops. */
    public List<Room> findByStatus(RoomStatus status) {
        return store.values().stream()
            .flatMap(hotelRooms -> hotelRooms.values().stream())
            .filter(r -> r.getStatus() == status)
            .collect(Collectors.toList());
    }

    /** Remove a single room — O(1) via secondary index. */
    public void delete(String roomId) {
        String hotelId = roomHotelIndex.remove(roomId);
        if (hotelId != null) {
            Map<String, Room> hotelRooms = store.get(hotelId);
            if (hotelRooms != null) hotelRooms.remove(roomId);
        }
    }

    /** Remove all rooms for a hotel — O(1) single map.remove(). */
    public void deleteAllByHotelId(String hotelId) {
        Map<String, Room> removed = store.remove(hotelId);
        if (removed != null) {
            removed.keySet().forEach(roomHotelIndex::remove);
        }
    }

    public List<Room> findAll() {
        return store.values().stream()
            .flatMap(m -> m.values().stream())
            .collect(Collectors.toList());
    }
}

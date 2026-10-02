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
 * Stores all Room objects across all hotels (global store).
 * Separate from HotelRepository - Hotel is thin, rooms are queried independently.
 *
 * Lifecycle methods  (used by RoomService):
 *   save, findById, findByHotelId, findByStatus, delete, findAll
 *
 * Query methods (used by SearchService ONLY - not by RoomService):
 *   findAvailableByHotelIdAndType
 */
public class RoomRepository {

    private final Map<String, Room> store = new ConcurrentHashMap<>();

    // ── Lifecycle methods (RoomService uses these) ────────────────────────────

    public void save(Room room) {
        store.put(room.getRoomId(), room);
    }

    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(store.get(roomId));
    }

    public List<Room> findByHotelId(String hotelId) {
        return store.values().stream()
            .filter(r -> r.getHotelId().equals(hotelId))
            .collect(Collectors.toList());
    }

    public List<Room> findByStatus(RoomStatus status) {
        return store.values().stream()
            .filter(r -> r.getStatus() == status)
            .collect(Collectors.toList());
    }

    public void delete(String roomId) {
        store.remove(roomId);
    }

    public List<Room> findAll() {
        return new ArrayList<>(store.values());
    }

    // ── Query methods (SearchService uses these - NOT RoomService) ───────────

    public List<Room> findAvailableByHotelIdAndType(String hotelId, RoomType type) {
        return store.values().stream()
            .filter(r -> r.getHotelId().equals(hotelId))
            .filter(r -> type == null || r.getType() == type)
            .filter(Room::isAvailable)
            .collect(Collectors.toList());
    }
}

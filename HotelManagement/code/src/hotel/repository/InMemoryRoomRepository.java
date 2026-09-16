package hotel.repository;

import hotel.model.Room;
import hotel.model.RoomType;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryRoomRepository implements RoomRepository {
    private final Map<String, Room> store = new HashMap<>();

    @Override
    public void save(Room room) { store.put(room.getRoomId(), room); }

    @Override
    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(store.get(roomId));
    }

    @Override
    public List<Room> findByHotelId(String hotelId) {
        return store.values().stream()
                .filter(r -> r.getHotelId().equals(hotelId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Room> findByHotelIdAndType(String hotelId, RoomType type) {
        return store.values().stream()
                .filter(r -> r.getHotelId().equals(hotelId) && r.getType() == type)
                .collect(Collectors.toList());
    }
}

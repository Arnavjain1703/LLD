package hotel.repository;

import hotel.model.Room;
import hotel.model.RoomType;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    void save(Room room);
    Optional<Room> findById(String roomId);
    List<Room> findByHotelId(String hotelId);
    List<Room> findByHotelIdAndType(String hotelId, RoomType type);
}

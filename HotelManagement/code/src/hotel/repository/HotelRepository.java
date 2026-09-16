package hotel.repository;

import hotel.model.Hotel;
import java.util.List;
import java.util.Optional;

public interface HotelRepository {
    void save(Hotel hotel);
    Optional<Hotel> findById(String hotelId);
    List<Hotel> findAll();
    List<Hotel> findByCity(String city);
}

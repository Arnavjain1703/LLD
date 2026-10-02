package repository;

import model.Hotel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stores Hotel objects only — no Room data here.
 * Thread-safe via ConcurrentHashMap.
 */
public class HotelRepository {
    // hotelId → Hotel
    private final Map<String, Hotel> store = new ConcurrentHashMap<>();

    public void save(Hotel hotel) {
        store.put(hotel.getHotelId(), hotel);
    }

    public Optional<Hotel> findById(String hotelId) {
        return Optional.ofNullable(store.get(hotelId));
    }

    /** Find all hotels in a given city (case-insensitive). */
    public List<Hotel> findByCity(String city) {
        return store.values().stream()
            .filter(h -> h.getAddress().getCity().equalsIgnoreCase(city))
            .collect(Collectors.toList());
    }

    /** Find hotels at or above a minimum star rating. */
    public List<Hotel> findByMinStarRating(int minStars) {
        return store.values().stream()
            .filter(h -> h.getStarRating() >= minStars)
            .collect(Collectors.toList());
    }

    public List<Hotel> findAll() {
        return new ArrayList<>(store.values());
    }

    public void delete(String hotelId) {
        store.remove(hotelId);
    }

    public boolean exists(String hotelId) {
        return store.containsKey(hotelId);
    }
}
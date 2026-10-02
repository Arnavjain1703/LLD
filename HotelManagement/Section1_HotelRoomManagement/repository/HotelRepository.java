package repository;

import model.Hotel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stores Hotel objects. Thread-safe via ConcurrentHashMap.
 *
 * Lifecycle methods  (used by HotelService):
 *   save, findById, exists, delete, findAll
 *
 * Query methods (used by SearchService ONLY - not by HotelService):
 *   findByCity, findByMinStarRating
 */
public class HotelRepository {

    private final Map<String, Hotel> store = new ConcurrentHashMap<>();

    // ── Lifecycle methods (HotelService uses these) ───────────────────────────

    public void save(Hotel hotel) {
        store.put(hotel.getHotelId(), hotel);
    }

    public Optional<Hotel> findById(String hotelId) {
        return Optional.ofNullable(store.get(hotelId));
    }

    public boolean exists(String hotelId) {
        return store.containsKey(hotelId);
    }

    public void delete(String hotelId) {
        store.remove(hotelId);
    }

    public List<Hotel> findAll() {
        return new ArrayList<>(store.values());
    }

    // ── Query methods (SearchService uses these - NOT HotelService) ──────────

    public List<Hotel> findByCity(String city) {
        return store.values().stream()
            .filter(h -> h.getAddress().getCity().equalsIgnoreCase(city))
            .collect(Collectors.toList());
    }

    public List<Hotel> findByMinStarRating(int minStars) {
        return store.values().stream()
            .filter(h -> h.getStarRating() >= minStars)
            .collect(Collectors.toList());
    }
}

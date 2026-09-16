package hotel.repository;

import hotel.model.Hotel;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryHotelRepository implements HotelRepository {
    private final Map<String, Hotel> store = new HashMap<>();

    @Override
    public void save(Hotel hotel) {
        store.put(hotel.getId(), hotel);
    }

    @Override
    public Optional<Hotel> findById(String hotelId) {
        return Optional.ofNullable(store.get(hotelId));
    }

    @Override
    public List<Hotel> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Hotel> findByCity(String city) {
        return store.values().stream()
                .filter(h -> h.getAddress().getCity().equalsIgnoreCase(city))
                .collect(Collectors.toList());
    }
}

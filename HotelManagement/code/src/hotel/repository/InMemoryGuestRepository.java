package hotel.repository;

import hotel.model.Guest;
import java.util.*;

public class InMemoryGuestRepository implements GuestRepository {
    private final Map<String, Guest> store = new HashMap<>();

    @Override
    public void save(Guest guest) { store.put(guest.getEmail(), guest); }

    @Override
    public Optional<Guest> findById(String email) {
        return Optional.ofNullable(store.get(email));
    }

    @Override
    public List<Guest> findAll() { return new ArrayList<>(store.values()); }

    @Override
    public boolean existsById(String email) { return store.containsKey(email); }
}

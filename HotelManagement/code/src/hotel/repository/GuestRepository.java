package hotel.repository;

import hotel.model.Guest;
import java.util.List;
import java.util.Optional;

public interface GuestRepository {
    void save(Guest guest);
    Optional<Guest> findById(String email);
    List<Guest> findAll();
    boolean existsById(String email);
}

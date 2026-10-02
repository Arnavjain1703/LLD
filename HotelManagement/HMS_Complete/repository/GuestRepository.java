package repository;

import model.Guest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GuestRepository — stores Guest objects with two lookup paths.
 *
 * Primary store : Map<guestId, Guest>
 *   - getGuest(id)   = O(1)
 *   - save / delete  = O(1)
 *
 * Secondary index: Map<email, guestId>
 *   - findByEmail    = O(1)
 *   - dedup guard on registration = O(1)
 *
 * Why a secondary index and not a linear scan on email?
 *   Email is the natural dedup key and is used on every login / booking lookup.
 *   O(1) vs O(n) matters at scale; maintaining the index is trivial.
 */
public class GuestRepository {

    private final Map<String, Guest>  store      = new ConcurrentHashMap<>(); // guestId -> Guest
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>(); // email -> guestId

    public void save(Guest guest) {
        store.put(guest.getGuestId(), guest);
        emailIndex.put(guest.getEmail(), guest.getGuestId());
    }

    public Optional<Guest> findById(String guestId) {
        return Optional.ofNullable(store.get(guestId));
    }

    /** O(1) email lookup via secondary index. */
    public Optional<Guest> findByEmail(String email) {
        String guestId = emailIndex.get(email);
        if (guestId == null) return Optional.empty();
        return Optional.ofNullable(store.get(guestId));
    }

    /** True if any guest already holds this email — used for dedup on register. */
    public boolean emailExists(String email) {
        return emailIndex.containsKey(email);
    }

    public void delete(String guestId) {
        Guest removed = store.remove(guestId);
        if (removed != null) emailIndex.remove(removed.getEmail());
    }

    public List<Guest> findAll() {
        return new ArrayList<>(store.values());
    }
}

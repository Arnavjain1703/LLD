package hotel.service;

import hotel.exception.GuestAlreadyExistsException;
import hotel.exception.GuestNotFoundException;
import hotel.model.Guest;
import hotel.repository.GuestRepository;
import java.util.List;

public class GuestService {
    private final GuestRepository guestRepository;

    public GuestService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    public Guest register(String email, String name, String phone) {
        if (guestRepository.existsById(email))
            throw new GuestAlreadyExistsException("Guest already registered: " + email);
        Guest guest = new Guest(email, name, phone);
        guestRepository.save(guest);
        return guest;
    }

    public Guest getGuest(String email) {
        return guestRepository.findById(email)
                .orElseThrow(() -> new GuestNotFoundException("Guest not found: " + email));
    }

    public List<Guest> getAllGuests() { return guestRepository.findAll(); }

    public void updateProfile(String email, String name, String phone) {
        Guest guest = getGuest(email);
        if (name  != null) guest.setName(name);
        if (phone != null) guest.setPhone(phone);
    }
}

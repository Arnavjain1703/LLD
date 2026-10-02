package service;

import model.Hotel;
import repository.HotelRepository;

import java.util.List;

/**
 * HotelService - hotel lifecycle ONLY.
 *
 * Responsibility: register, deregister, fetch hotels.
 * Actor: Chain Admin.
 *
 * Dependencies: HotelRepository only.
 * Does NOT know about RoomService - zero coupling between the two services.
 *
 * Cascade on deregister (removing rooms) is the caller's responsibility:
 *   Step 1: roomService.removeAllRoomsForHotel(hotelId)
 *   Step 2: hotelService.deregisterHotel(hotelId)
 * The HotelManagementSystem facade coordinates this order.
 */
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public void registerHotel(Hotel hotel) {
        if (hotelRepository.exists(hotel.getHotelId())) {
            throw new IllegalArgumentException("Hotel already registered: " + hotel.getHotelId());
        }
        hotelRepository.save(hotel);
        System.out.println("Registered: " + hotel);
    }

    /** Deletes the hotel record only. Caller must remove rooms first. */
    public void deregisterHotel(String hotelId) {
        getHotel(hotelId); // guard: must exist
        hotelRepository.delete(hotelId);
        System.out.println("Deregistered hotel: " + hotelId);
    }

    /** Direct ID lookup - used internally by other services. */
    public Hotel getHotel(String hotelId) {
        return hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + hotelId));
    }

    /** Admin view - all hotels regardless of availability. */
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }
}

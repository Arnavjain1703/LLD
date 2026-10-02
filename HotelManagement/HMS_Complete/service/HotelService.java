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
 * Dependencies: HotelRepository only. Does NOT know about RoomService.
 *
 * Cascade (removing rooms on hotel deregister) is the caller's job:
 *   1. roomService.removeAllRoomsForHotel(id)  // caller does this first
 *   2. hotelService.deregisterHotel(id)         // then this
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

    /** Admin view - all hotels in the chain regardless of availability. */
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }
}

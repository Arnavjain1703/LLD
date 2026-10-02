package service;

import model.*;
import repository.HotelRepository;
import repository.RoomRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SearchService -- all discovery and availability queries.
 *
 * Responsibility: Answer "what hotels/rooms exist and are available?"
 * Actor: Guest (primary), Front Desk (walk-in availability check).
 *
 * Rule: If the operation queries or discovers a resource, it lives here.
 *       Lifecycle mutations (register, add, mark maintenance) live in the
 *       respective management services.
 *
 * Two levels of search:
 *   Level 1 -- Browse  : no availability check, just filter by attributes
 *   Level 2 -- Availability: filter by attributes + room status
 *
 * NOTE: Booking overlap check (is the room already booked for these dates?)
 *   is NOT implemented here yet. It requires BookingRepository which belongs
 *   to Section 4. That check will be added to getAvailableRooms() once
 *   BookingRepository is in place:
 *
 *     .filter(r -> hasNoOverlappingBooking(r.getRoomId(), checkIn, checkOut))
 *
 *   For now, status=AVAILABLE is the sole availability signal.
 */
public class SearchService {

    private final HotelRepository hotelRepository;
    private final RoomRepository  roomRepository;

    public SearchService(HotelRepository hotelRepository,
                         RoomRepository  roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository  = roomRepository;
    }

    // -- Level 1: Browse (no date / availability check) -----------------------

    /**
     * All hotels in a city -- browse mode.
     * Guest sees all options before picking dates.
     * Lives here (not HotelService) because this is discovery, not management.
     */
    public List<Hotel> getHotelsByCity(String city) {
        return hotelRepository.findByCity(city);
    }

    /**
     * Hotels at or above a star rating across the chain.
     * Lives here (not HotelService) because this is discovery, not management.
     */
    public List<Hotel> getHotelsByMinStars(int minStars) {
        return hotelRepository.findByMinStarRating(minStars);
    }

    // -- Level 2: Availability (attributes + status filter) -------------------

    /**
     * Hotels in a city that have at least one room matching all criteria.
     * Returns one SearchResult per hotel (hotel + its matching available rooms).
     *
     * Guest flow: city + dates -> hotels with rooms -> pick hotel -> pick room -> book
     */
    public List<SearchResult> searchHotels(SearchCriteria criteria) {
        return hotelRepository.findByCity(criteria.getCity()).stream()
            .map(hotel -> new SearchResult(hotel,
                             getAvailableRooms(hotel.getHotelId(), criteria)))
            .filter(result -> result.getRoomCount() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Available rooms within a specific hotel matching all criteria.
     * Called after guest picks a hotel from searchHotels().
     */
    public List<Room> searchRoomsInHotel(String hotelId, SearchCriteria criteria) {
        return getAvailableRooms(hotelId, criteria);
    }

    // -- Internal -------------------------------------------------------------

    /**
     * Core filter applied per hotel.
     *
     * Filter chain (cheapest checks first):
     *   1. type filter      -- enum comparison, O(1)
     *   2. capacity filter  -- int comparison, O(1)
     *   3. status check     -- enum comparison, O(1); rejects UNDER_MAINTENANCE
     *
     * TODO (Section 4): add booking overlap check as step 4:
     *   .filter(r -> hasNoOverlappingBooking(r.getRoomId(), checkIn, checkOut))
     *   That step is the most expensive (hits BookingRepository) so it goes last.
     */
    private List<Room> getAvailableRooms(String hotelId, SearchCriteria criteria) {
        return roomRepository.findByHotelId(hotelId).stream()
            .filter(r -> criteria.getRoomType() == null
                      || r.getType() == criteria.getRoomType())
            .filter(r -> r.getCapacity() >= criteria.getMinCapacity())
            .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
            .collect(Collectors.toList());
    }
}

package hotel.search;

import hotel.model.Hotel;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.repository.BookingRepository;
import hotel.repository.HotelRepository;
import hotel.repository.RoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Searches across ALL hotels.
 * Filter chain: city → minRating → roomType → date availability.
 */
public class SearchService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public SearchService(HotelRepository hotelRepository,
                         RoomRepository roomRepository,
                         BookingRepository bookingRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<SearchResult> search(SearchCriteria criteria) {
        List<Hotel> hotels = hotelRepository.findAll();

        // 1. filter by city
        if (criteria.getCity() != null) {
            hotels = hotels.stream()
                    .filter(h -> h.getAddress().getCity().equalsIgnoreCase(criteria.getCity()))
                    .collect(Collectors.toList());
        }

        // 2. filter by minimum rating
        if (criteria.getMinRating() != null) {
            hotels = hotels.stream()
                    .filter(h -> h.getRating() >= criteria.getMinRating())
                    .collect(Collectors.toList());
        }

        List<SearchResult> results = new ArrayList<>();

        for (Hotel hotel : hotels) {
            List<Room> rooms = roomRepository.findByHotelId(hotel.getId());

            // 3. filter by room type
            if (criteria.getRoomType() != null) {
                rooms = rooms.stream()
                        .filter(r -> r.getType() == criteria.getRoomType())
                        .collect(Collectors.toList());
            }

            // 4. filter by date availability
            if (criteria.getCheckIn() != null && criteria.getCheckOut() != null) {
                rooms = rooms.stream()
                        .filter(r -> isAvailable(r, criteria))
                        .collect(Collectors.toList());
            } else {
                // no date filter — only show AVAILABLE rooms
                rooms = rooms.stream()
                        .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                        .collect(Collectors.toList());
            }

            for (Room room : rooms) {
                results.add(new SearchResult(hotel, room));
            }
        }

        return results;
    }

    private boolean isAvailable(Room room, SearchCriteria criteria) {
        // room must not be under maintenance
        if (room.getStatus() == RoomStatus.MAINTENANCE) return false;

        // no active booking should overlap the requested window
        return bookingRepository.findActiveByRoom(room.getRoomId()).stream()
                .noneMatch(b -> b.overlaps(criteria.getCheckIn(), criteria.getCheckOut()));
    }
}

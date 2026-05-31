package com.hotelmanagement.service;

import com.hotelmanagement.enums.RoomType;
import com.hotelmanagement.models.Hotel;
import com.hotelmanagement.models.Room;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Optimized search service using:
 * - ConcurrentHashMap<RoomType, TreeSet<Room>> for O(log n) price-sorted type queries
 * - ConcurrentHashMap<String, List<Hotel>> for O(1) location-based lookup
 * - Delegates date-range availability to RoomManager's TreeMap-based check
 */
public class SearchService {
    private final RoomManager roomManager;
    // RoomType -> TreeSet sorted by price (Room implements Comparable)
    private final ConcurrentHashMap<RoomType, TreeSet<Room>> roomsByType;
    // location (lowercase) -> list of hotels
    private final ConcurrentHashMap<String, List<Hotel>> hotelsByLocation;

    public SearchService(RoomManager roomManager) {
        this.roomManager = roomManager;
        this.roomsByType = new ConcurrentHashMap<>();
        this.hotelsByLocation = new ConcurrentHashMap<>();
    }

    public void indexHotel(Hotel hotel) {
        hotelsByLocation.computeIfAbsent(hotel.getLocation().toLowerCase(), k -> new ArrayList<>())
                .add(hotel);
        for (Room room : hotel.getAllRooms()) {
            indexRoom(room);
        }
    }

    public void indexRoom(Room room) {
        roomsByType.computeIfAbsent(room.getType(), k -> new TreeSet<>()).add(room);
    }

    /**
     * Search available rooms by hotel, date range, and optional type.
     * Complexity: O(R × log B) where R = rooms, B = bookings per room
     */
    public List<Room> searchAvailableRooms(String hotelId, LocalDate checkIn,
                                           LocalDate checkOut, RoomType type) {
        return roomManager.getAvailableRooms(hotelId, checkIn, checkOut, type);
    }

    /**
     * Search rooms by type within a price range.
     * Uses TreeSet (sorted by price) for O(log n) subset extraction.
     */
    public List<Room> searchByTypeAndPriceRange(RoomType type, double minPrice, double maxPrice,
                                                LocalDate checkIn, LocalDate checkOut) {
        TreeSet<Room> rooms = roomsByType.get(type);
        if (rooms == null) return Collections.emptyList();

        // Create dummy rooms for range bounds
        Room low = new Room("_low", "", 0, 0, type, minPrice);
        Room high = new Room("_high_zzz", "", 0, 0, type, maxPrice);

        // O(log n) subset using TreeSet's subSet
        return rooms.subSet(low, true, high, true).stream()
                .filter(r -> roomManager.isRoomAvailable(r.getId(), checkIn, checkOut))
                .collect(Collectors.toList());
    }

    /**
     * Search hotels by location. O(1) lookup.
     */
    public List<Hotel> searchByLocation(String location) {
        return hotelsByLocation.getOrDefault(location.toLowerCase(), Collections.emptyList());
    }

    /**
     * Get cheapest available room across all hotels for given criteria.
     * Uses PriorityQueue for O(1) min extraction.
     */
    public Optional<Room> findCheapestAvailable(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        TreeSet<Room> rooms = roomsByType.get(type);
        if (rooms == null) return Optional.empty();

        // TreeSet is already sorted by price; first available is cheapest
        return rooms.stream()
                .filter(r -> roomManager.isRoomAvailable(r.getId(), checkIn, checkOut))
                .findFirst();
    }
}

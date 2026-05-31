package com.hotelmanagement.service;

import com.hotelmanagement.enums.RoomStatus;
import com.hotelmanagement.enums.RoomType;
import com.hotelmanagement.exceptions.RoomNotAvailableException;
import com.hotelmanagement.models.Hotel;
import com.hotelmanagement.models.Room;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe room allocation manager.
 * Uses TreeMap per room for O(log n) date-overlap detection.
 * Uses ReentrantLock per room for fine-grained concurrency.
 */
public class RoomManager {
    // roomId -> sorted reservation intervals (checkIn -> checkOut)
    private final ConcurrentHashMap<String, TreeMap<LocalDate, LocalDate>> roomReservations;
    private final ConcurrentHashMap<String, Room> roomRegistry;
    private final ConcurrentHashMap<String, Hotel> hotelRegistry;

    public RoomManager() {
        this.roomReservations = new ConcurrentHashMap<>();
        this.roomRegistry = new ConcurrentHashMap<>();
        this.hotelRegistry = new ConcurrentHashMap<>();
    }

    public void addHotel(Hotel hotel) {
        hotelRegistry.put(hotel.getId(), hotel);
        for (Room room : hotel.getAllRooms()) {
            registerRoom(room);
        }
    }

    public void registerRoom(Room room) {
        roomRegistry.put(room.getId(), room);
        roomReservations.putIfAbsent(room.getId(), new TreeMap<>());
    }

    /**
     * Atomically allocates a room for given dates.
     * Uses per-room ReentrantLock to prevent concurrent double-booking.
     * Date overlap detection: O(log n) using TreeMap floor/ceiling.
     */
    public boolean allocateRoom(String roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = roomRegistry.get(roomId);
        if (room == null) return false;

        // Acquire per-room lock for atomic check-and-allocate
        if (!room.acquireLock()) {
            return false; // Another thread holds the lock
        }
        try {
            if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE) return false;
            if (!isAvailableInternal(roomId, checkIn, checkOut)) return false;

            // Insert reservation into TreeMap
            TreeMap<LocalDate, LocalDate> reservations = roomReservations.get(roomId);
            reservations.put(checkIn, checkOut);
            return true;
        } finally {
            room.releaseLock();
        }
    }

    /**
     * Releases a room reservation for given dates.
     */
    public void releaseRoom(String roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = roomRegistry.get(roomId);
        if (room == null) return;

        if (!room.acquireLock()) {
            // Spin-wait briefly for release operations (critical for hold expiry)
            try { room.lockInterruptibly(); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        try {
            TreeMap<LocalDate, LocalDate> reservations = roomReservations.get(roomId);
            if (reservations != null) {
                reservations.remove(checkIn);
            }
        } finally {
            room.releaseLock();
        }
    }

    /**
     * O(log n) overlap detection using TreeMap floor/ceiling.
     * Two intervals [A_start, A_end) and [B_start, B_end) overlap iff:
     *   A_start < B_end AND B_start < A_end
     * Note: checkOut day is NOT considered occupied (guest leaves by 11 AM).
     */
    public boolean isRoomAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = roomRegistry.get(roomId);
        if (room == null || room.getStatus() == RoomStatus.UNDER_MAINTENANCE) return false;
        return isAvailableInternal(roomId, checkIn, checkOut);
    }

    private boolean isAvailableInternal(String roomId, LocalDate checkIn, LocalDate checkOut) {
        TreeMap<LocalDate, LocalDate> reservations = roomReservations.get(roomId);
        if (reservations == null || reservations.isEmpty()) return true;

        // Check reservation starting at or just before checkIn
        Map.Entry<LocalDate, LocalDate> lower = reservations.floorEntry(checkIn);
        if (lower != null && lower.getValue().isAfter(checkIn)) {
            return false; // Existing reservation's checkout is after our checkIn
        }

        // Check reservation starting just after checkIn
        Map.Entry<LocalDate, LocalDate> upper = reservations.ceilingEntry(checkIn);
        if (upper != null && upper.getKey().isBefore(checkOut)) {
            return false; // Next reservation starts before our checkOut
        }

        return true;
    }

    /**
     * Returns all available rooms for a hotel within a date range, optionally filtered by type.
     */
    public List<Room> getAvailableRooms(String hotelId, LocalDate checkIn, LocalDate checkOut, RoomType type) {
        Hotel hotel = hotelRegistry.get(hotelId);
        if (hotel == null) return Collections.emptyList();

        return hotel.getAllRooms().stream()
                .filter(r -> type == null || r.getType() == type)
                .filter(r -> r.getStatus() != RoomStatus.UNDER_MAINTENANCE)
                .filter(r -> isAvailableInternal(r.getId(), checkIn, checkOut))
                .sorted() // sorted by price (Room implements Comparable)
                .collect(Collectors.toList());
    }

    public Room getRoom(String roomId) { return roomRegistry.get(roomId); }
    public Hotel getHotel(String hotelId) { return hotelRegistry.get(hotelId); }
    public Collection<Hotel> getAllHotels() { return hotelRegistry.values(); }
}

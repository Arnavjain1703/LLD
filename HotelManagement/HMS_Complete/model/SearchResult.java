package model;

import java.util.List;

/**
 * One result per hotel — shows which hotel has available rooms matching the criteria.
 * Guest sees the hotel details and the specific rooms they can pick from.
 */
public class SearchResult {
    private final Hotel      hotel;
    private final List<Room> availableRooms;

    public SearchResult(Hotel hotel, List<Room> availableRooms) {
        this.hotel          = hotel;
        this.availableRooms = List.copyOf(availableRooms);
    }

    public Hotel      getHotel()          { return hotel; }
    public List<Room> getAvailableRooms() { return availableRooms; }
    public int        getRoomCount()      { return availableRooms.size(); }

    @Override
    public String toString() {
        return hotel + " — " + availableRooms.size() + " room(s) available";
    }
}
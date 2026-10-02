package model;

import java.util.List;

/**
 * One result per hotel — bundles the hotel with the rooms that matched
 * the search criteria and are currently available.
 *
 * Immutable snapshot: List.copyOf() so callers cannot mutate the result.
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
        return hotel + " -- " + availableRooms.size() + " room(s) available";
    }
}

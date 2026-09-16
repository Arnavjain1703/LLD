package hotel.search;

import hotel.model.Hotel;
import hotel.model.Room;

public class SearchResult {
    private final Hotel hotel;
    private final Room room;

    public SearchResult(Hotel hotel, Room room) {
        this.hotel = hotel;
        this.room = room;
    }

    public Hotel getHotel() { return hotel; }
    public Room getRoom()   { return room; }

    @Override
    public String toString() {
        return "SearchResult{hotel='" + hotel.getName() + "', room=" + room + "}";
    }
}

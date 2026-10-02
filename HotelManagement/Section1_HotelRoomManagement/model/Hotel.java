package model;

/**
 * Hotel is a thin entity - it does NOT hold List<Room>.
 * Rooms are owned by RoomRepository and reference Hotel via hotelId.
 *
 * Why thin?
 *  - Loading a hotel should not force loading 200+ rooms into memory.
 *  - Room queries (availability, maintenance) are independent of Hotel.
 */
public class Hotel {
    private final String  hotelId;
    private final String  name;
    private final Address address;
    private final int     starRating; // 1-5

    public Hotel(String hotelId, String name, Address address, int starRating) {
        if (starRating < 1 || starRating > 5) {
            throw new IllegalArgumentException("Star rating must be between 1 and 5");
        }
        this.hotelId    = hotelId;
        this.name       = name;
        this.address    = address;
        this.starRating = starRating;
    }

    public String  getHotelId()    { return hotelId; }
    public String  getName()       { return name; }
    public Address getAddress()    { return address; }
    public int     getStarRating() { return starRating; }

    @Override
    public String toString() {
        return name + " (" + starRating + "stars) - " + address.getCity();
    }
}

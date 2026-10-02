package model;

import java.time.LocalDate;

/**
 * Encapsulates what the guest passes to SearchService.
 * Optional fields: roomType (null = any), minCapacity (default 1).
 *
 * Using a criteria object instead of 5 method params keeps
 * SearchService signatures stable when we add new filters.
 */
public class SearchCriteria {
    private final String    city;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final RoomType  roomType;    // null = any type
    private final int       minCapacity; // default 1

    private SearchCriteria(Builder b) {
        this.city        = b.city;
        this.checkIn     = b.checkIn;
        this.checkOut    = b.checkOut;
        this.roomType    = b.roomType;
        this.minCapacity = b.minCapacity;
    }

    public String    getCity()        { return city; }
    public LocalDate getCheckIn()     { return checkIn; }
    public LocalDate getCheckOut()    { return checkOut; }
    public RoomType  getRoomType()    { return roomType; }
    public int       getMinCapacity() { return minCapacity; }

    // Builder — required fields in constructor, optional ones via fluent setters
    public static class Builder {
        private final String    city;
        private final LocalDate checkIn;
        private final LocalDate checkOut;
        private RoomType roomType    = null;
        private int      minCapacity = 1;

        public Builder(String city, LocalDate checkIn, LocalDate checkOut) {
            if (city == null || city.isBlank()) throw new IllegalArgumentException("City required");
            if (!checkOut.isAfter(checkIn))     throw new IllegalArgumentException("Check-out must be after check-in");
            this.city     = city;
            this.checkIn  = checkIn;
            this.checkOut = checkOut;
        }

        public Builder roomType(RoomType type)    { this.roomType    = type; return this; }
        public Builder minCapacity(int capacity)  { this.minCapacity = capacity; return this; }
        public SearchCriteria build()             { return new SearchCriteria(this); }
    }

    @Override
    public String toString() {
        return "Search[city=" + city + ", " + checkIn + " to " + checkOut
             + (roomType != null ? ", type=" + roomType : "")
             + ", minCap=" + minCapacity + "]";
    }
}
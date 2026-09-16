package hotel.search;

import hotel.model.RoomType;
import java.time.LocalDate;

/** Immutable value object — build via SearchCriteria.Builder. */
public class SearchCriteria {
    private final String city;
    private final Double minRating;
    private final RoomType roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    private SearchCriteria(Builder b) {
        this.city = b.city;
        this.minRating = b.minRating;
        this.roomType = b.roomType;
        this.checkIn = b.checkIn;
        this.checkOut = b.checkOut;
    }

    public String getCity()        { return city; }
    public Double getMinRating()   { return minRating; }
    public RoomType getRoomType()  { return roomType; }
    public LocalDate getCheckIn()  { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }

    public static class Builder {
        private String city;
        private Double minRating;
        private RoomType roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;

        public Builder city(String city)             { this.city = city; return this; }
        public Builder minRating(double r)           { this.minRating = r; return this; }
        public Builder roomType(RoomType t)          { this.roomType = t; return this; }
        public Builder checkIn(LocalDate d)          { this.checkIn = d; return this; }
        public Builder checkOut(LocalDate d)         { this.checkOut = d; return this; }
        public SearchCriteria build()                { return new SearchCriteria(this); }
    }
}

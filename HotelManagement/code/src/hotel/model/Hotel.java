package hotel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hotel {
    private final String id;
    private final String name;
    private final Address address;
    private double rating;
    private final List<Room> rooms;

    public Hotel(String id, String name, Address address, double rating) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.rooms = new ArrayList<>();
    }

    public String getId()       { return id; }
    public String getName()     { return name; }
    public Address getAddress() { return address; }
    public double getRating()   { return rating; }

    public void setRating(double rating) { this.rating = rating; }

    public void addRoom(Room room)           { rooms.add(room); }
    public List<Room> getRooms()             { return Collections.unmodifiableList(rooms); }

    @Override
    public String toString() {
        return "Hotel{id='" + id + "', name='" + name + "', city='" + address.getCity() + "', rating=" + rating + "}";
    }
}

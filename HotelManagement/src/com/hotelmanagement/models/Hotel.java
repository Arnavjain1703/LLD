package com.hotelmanagement.models;

import com.hotelmanagement.enums.RoomType;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Hotel {
    private final String id;
    private final String name;
    private final String location;
    private final ConcurrentHashMap<String, Room> rooms; // roomId -> Room

    public Hotel(String id, String name, String location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.rooms = new ConcurrentHashMap<>();
    }

    public void addRoom(Room room) { rooms.put(room.getId(), room); }
    public void removeRoom(String roomId) { rooms.remove(roomId); }
    public Room getRoom(String roomId) { return rooms.get(roomId); }

    public List<Room> getRoomsByType(RoomType type) {
        return rooms.values().stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    public List<Room> getAllRooms() { return List.copyOf(rooms.values()); }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
}

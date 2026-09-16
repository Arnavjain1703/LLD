package hotel.service;

import hotel.exception.HotelNotFoundException;
import hotel.exception.RoomNotFoundException;
import hotel.model.Hotel;
import hotel.model.Room;
import hotel.model.RoomType;
import hotel.repository.HotelRepository;
import hotel.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

public class HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    public void addHotel(Hotel hotel) {
        hotelRepository.save(hotel);
    }

    public Hotel getHotel(String hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException("Hotel not found: " + hotelId));
    }

    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    public List<Hotel> getHotelsByCity(String city) {
        return hotelRepository.findByCity(city);
    }

    public void addRoom(String hotelId, Room room) {
        Hotel hotel = getHotel(hotelId);   // validates hotel exists
        hotel.addRoom(room);
        roomRepository.save(room);
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));
    }

    public List<Room> getRoomsByHotel(String hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    public List<Room> getRoomsByType(String hotelId, RoomType type) {
        return roomRepository.findByHotelIdAndType(hotelId, type);
    }

    public void updateRating(String hotelId, double newRating) {
        Hotel hotel = getHotel(hotelId);
        hotel.setRating(newRating);
    }
}

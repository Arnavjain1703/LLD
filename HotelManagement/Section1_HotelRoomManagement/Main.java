import model.*;
import repository.*;
import service.*;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {

        HotelRepository hotelRepo = new HotelRepository();
        RoomRepository  roomRepo  = new RoomRepository();

        HotelService hotelService = new HotelService(hotelRepo);
        RoomService  roomService  = new RoomService(roomRepo, hotelRepo);

        // Register hotels
        hotelService.registerHotel(new Hotel("H1", "Marriott Mumbai",
            new Address("MG Road", "Mumbai", "India", "400001"), 5));
        hotelService.registerHotel(new Hotel("H2", "Ibis Mumbai",
            new Address("Andheri", "Mumbai", "India", "400053"), 3));
        hotelService.registerHotel(new Hotel("H3", "Taj Delhi",
            new Address("Connaught Place", "Delhi", "India", "110001"), 5));

        // Add rooms — hotelId passed separately, Room has no hotelId field
        roomService.addRoom("H1", new Room("R101", "101", 1, RoomType.SINGLE,  2, new BigDecimal("4500")));
        roomService.addRoom("H1", new Room("R201", "201", 2, RoomType.DOUBLE,  4, new BigDecimal("7500")));
        roomService.addRoom("H1", new Room("R501", "501", 5, RoomType.SUITE,   2, new BigDecimal("15000")));
        roomService.addRoom("H2", new Room("R001", "001", 0, RoomType.SINGLE,  2, new BigDecimal("2500")));
        roomService.addRoom("H3", new Room("T101", "101", 1, RoomType.SUITE,   2, new BigDecimal("12000")));

        System.out.println("
=== All rooms in Marriott (H1) ===" );
        roomService.getRoomsByHotel("H1").forEach(System.out::println);

        System.out.println("
=== Mark R101 for maintenance ===");
        roomService.markUnderMaintenance("R101");
        System.out.println("R101 status: " + roomService.getRoom("R101").getStatus());

        System.out.println("
=== Restore R101 ===");
        roomService.markAvailable("R101");

        // Guard: cannot mark OCCUPIED room
        System.out.println("
=== Guard: mark occupied room for maintenance ===");
        roomService.getRoom("R201").setStatus(RoomStatus.OCCUPIED);
        try {
            roomService.markUnderMaintenance("R201");
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // Guard: add room to non-existent hotel
        System.out.println("
=== Guard: add room to ghost hotel ===");
        try {
            roomService.addRoom("GHOST", new Room("X999", "999", 9, RoomType.SUITE, 2, new BigDecimal("99999")));
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // Deregister: facade coordinates cascade
        System.out.println("
=== Deregister Taj Delhi ===");
        roomService.removeAllRoomsForHotel("H3");
        hotelService.deregisterHotel("H3");
        System.out.println("Remaining hotels: " + hotelService.getAllHotels().size());
    }
}

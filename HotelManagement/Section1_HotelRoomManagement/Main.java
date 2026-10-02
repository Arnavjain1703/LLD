import model.*;
import repository.*;
import service.*;
import java.math.BigDecimal;

/**
 * Section 1 Demo - Hotel and Room Management.
 * Shows HotelService and RoomService with zero coupling between them.
 */
public class Main {
    public static void main(String[] args) {

        // ── Repositories ──────────────────────────────────────────────────────
        HotelRepository hotelRepo = new HotelRepository();
        RoomRepository  roomRepo  = new RoomRepository();

        // ── Services - no dependency between HotelService and RoomService ─────
        HotelService hotelService = new HotelService(hotelRepo);
        RoomService  roomService  = new RoomService(roomRepo, hotelRepo);

        // ── Register hotels (HotelService) ───────────────────────────────────
        hotelService.registerHotel(new Hotel("H1", "Marriott Mumbai",
            new Address("MG Road", "Mumbai", "India", "400001"), 5));
        hotelService.registerHotel(new Hotel("H2", "Ibis Mumbai",
            new Address("Andheri", "Mumbai", "India", "400053"), 3));
        hotelService.registerHotel(new Hotel("H3", "Taj Delhi",
            new Address("Connaught Place", "Delhi", "India", "110001"), 5));

        // ── Add rooms (RoomService) ───────────────────────────────────────────
        roomService.addRoom(new Room("R101", "H1", "101", 1, RoomType.SINGLE,  2, new BigDecimal("4500")));
        roomService.addRoom(new Room("R201", "H1", "201", 2, RoomType.DOUBLE,  4, new BigDecimal("7500")));
        roomService.addRoom(new Room("R501", "H1", "501", 5, RoomType.SUITE,   2, new BigDecimal("15000")));
        roomService.addRoom(new Room("R001", "H2", "001", 0, RoomType.SINGLE,  2, new BigDecimal("2500")));
        roomService.addRoom(new Room("T101", "H3", "101", 1, RoomType.SUITE,   2, new BigDecimal("12000")));

        // ── Admin view: all hotels, all rooms ─────────────────────────────────
        System.out.println("\n=== All Hotels ===");
        hotelService.getAllHotels().forEach(System.out::println);

        System.out.println("\n=== All rooms in Marriott (H1) ===");
        roomService.getRoomsByHotel("H1").forEach(System.out::println);

        // ── Maintenance lifecycle (RoomService) ───────────────────────────────
        System.out.println("\n=== Mark R101 for maintenance ===");
        roomService.markUnderMaintenance("R101");
        System.out.println("R101 status: " + roomService.getRoom("R101").getStatus());

        System.out.println("\n=== Restore R101 ===");
        roomService.markAvailable("R101");
        System.out.println("R101 status: " + roomService.getRoom("R101").getStatus());

        // ── Guard: add room to non-existent hotel ─────────────────────────────
        System.out.println("\n=== Guard: add room to ghost hotel ===");
        try {
            roomService.addRoom(new Room("X999", "GHOST", "999", 9,
                RoomType.SUITE, 2, new BigDecimal("99999")));
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // ── Guard: mark OCCUPIED room for maintenance ─────────────────────────
        System.out.println("\n=== Guard: mark occupied room for maintenance ===");
        roomService.getRoom("R201").setStatus(RoomStatus.OCCUPIED); // simulate check-in
        try {
            roomService.markUnderMaintenance("R201");
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // ── Deregister: facade coordinates cascade manually ───────────────────
        System.out.println("\n=== Deregister Taj Delhi (cascade coordinated by caller) ===");
        roomService.removeAllRoomsForHotel("H3");   // step 1: rooms first
        hotelService.deregisterHotel("H3");          // step 2: hotel after
        System.out.println("Remaining hotels: " + hotelService.getAllHotels().size()); // 2
    }
}

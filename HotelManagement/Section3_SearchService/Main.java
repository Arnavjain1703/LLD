import model.*;
import repository.*;
import service.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        // -- Repositories
        HotelRepository hotelRepo = new HotelRepository();
        RoomRepository  roomRepo  = new RoomRepository();

        // -- Services
        HotelService  hotelService  = new HotelService(hotelRepo);
        RoomService   roomService   = new RoomService(roomRepo, hotelRepo);
        SearchService searchService = new SearchService(hotelRepo, roomRepo);

        // -- Seed hotels
        hotelService.registerHotel(new Hotel("H1", "Marriott Mumbai",
            new Address("MG Road",        "Mumbai", "India", "400001"), 5));
        hotelService.registerHotel(new Hotel("H2", "Ibis Mumbai",
            new Address("Andheri",        "Mumbai", "India", "400053"), 3));
        hotelService.registerHotel(new Hotel("H3", "Taj Delhi",
            new Address("Connaught Place","Delhi",  "India", "110001"), 5));

        // -- Seed rooms
        roomService.addRoom("H1", new Room("R101", "101", 1, RoomType.SINGLE, 2, new BigDecimal("4500")));
        roomService.addRoom("H1", new Room("R201", "201", 2, RoomType.DOUBLE, 4, new BigDecimal("7500")));
        roomService.addRoom("H1", new Room("R501", "501", 5, RoomType.SUITE,  2, new BigDecimal("15000")));
        roomService.addRoom("H2", new Room("R001", "001", 0, RoomType.SINGLE, 2, new BigDecimal("2500")));
        roomService.addRoom("H3", new Room("T101", "101", 1, RoomType.SUITE,  2, new BigDecimal("12000")));

        LocalDate checkIn  = LocalDate.of(2025, 12, 20);
        LocalDate checkOut = LocalDate.of(2025, 12, 25);

        // -- Level 1: Browse by city (no date filter)
        System.out.println("=== Browse: Hotels in Mumbai ===");
        searchService.getHotelsByCity("Mumbai").forEach(System.out::println);

        // -- Level 1: Browse by star rating
        System.out.println("\n=== Browse: 5-star hotels ===");
        searchService.getHotelsByMinStars(5).forEach(System.out::println);

        // -- Level 2: Search with criteria (type + capacity + status)
        System.out.println("\n=== Search: SINGLE rooms in Mumbai (Dec 20-25) ===");
        SearchCriteria criteria = new SearchCriteria.Builder("Mumbai", checkIn, checkOut)
            .roomType(RoomType.SINGLE).build();
        searchService.searchHotels(criteria).forEach(result -> {
            System.out.println(result);
            result.getAvailableRooms().forEach(r -> System.out.println("  " + r));
        });

        // -- Mark R101 for maintenance -- should disappear from search
        System.out.println("\n=== Mark R101 for maintenance ===");
        roomService.markUnderMaintenance("R101");
        searchService.searchHotels(criteria).forEach(result -> {
            System.out.println(result);
            result.getAvailableRooms().forEach(r -> System.out.println("  " + r));
        });

        // -- Search all room types in Mumbai
        System.out.println("\n=== Search: any room in Mumbai (Dec 20-25) ===");
        SearchCriteria anyCriteria = new SearchCriteria.Builder("Mumbai", checkIn, checkOut).build();
        searchService.searchHotels(anyCriteria).forEach(result -> {
            System.out.println(result);
            result.getAvailableRooms().forEach(r -> System.out.println("  " + r));
        });

        // -- TODO (Section 4): once BookingRepository exists, add overlap check:
        // -- Book R001, then search again -- R001 should not appear
        System.out.println("\n[Overlap check will be added in Section 4 -- BookingRepository]");
    }
}

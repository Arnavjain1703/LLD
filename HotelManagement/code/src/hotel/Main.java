package hotel;

import hotel.model.*;
import hotel.pricing.WeekendPricingStrategy;
import hotel.search.SearchCriteria;
import hotel.search.SearchResult;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        HotelManagementSystem hms = HotelManagementSystem.getInstance();

        // ─── 1. Add hotels ───────────────────────────────────────────────
        Address addr1 = new Address("123 MG Road", "Bangalore", "KA", "India", "560001");
        Address addr2 = new Address("45 Marine Drive", "Mumbai", "MH", "India", "400001");

        Hotel hotelA = new Hotel("H1", "The Grand Bangalore", addr1, 4.5);
        Hotel hotelB = new Hotel("H2", "Sea Pearl Mumbai", addr2, 4.2);
        hms.hotelService.addHotel(hotelA);
        hms.hotelService.addHotel(hotelB);

        // ─── 2. Add rooms ─────────────────────────────────────────────────
        Room r101 = new Room("R101", "H1", 101, RoomType.SINGLE, 2500.0);
        Room r102 = new Room("R102", "H1", 102, RoomType.DOUBLE, 4000.0);
        Room r201 = new Room("R201", "H1", 201, RoomType.SUITE,  8000.0);
        Room r301 = new Room("R301", "H2", 301, RoomType.DOUBLE, 5000.0);

        hms.hotelService.addRoom("H1", r101);
        hms.hotelService.addRoom("H1", r102);
        hms.hotelService.addRoom("H1", r201);
        hms.hotelService.addRoom("H2", r301);

        // ─── 3. Register guests ───────────────────────────────────────────
        Guest alice = hms.guestService.register("alice@example.com", "Alice", "+91-9876543210");
        Guest bob   = hms.guestService.register("bob@example.com", "Bob", "+91-9123456789");
        System.out.println("Registered: " + alice);
        System.out.println("Registered: " + bob);

        // ─── 4. Search available rooms ────────────────────────────────────
        LocalDate checkIn  = LocalDate.of(2026, 10, 1);
        LocalDate checkOut = LocalDate.of(2026, 10, 5);

        SearchCriteria criteria = new SearchCriteria.Builder()
                .city("Bangalore")
                .minRating(4.0)
                .roomType(RoomType.DOUBLE)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .build();

        List<SearchResult> results = hms.search(criteria);
        System.out.println("\n=== Search Results ===");
        results.forEach(System.out::println);

        // ─── 5. Book a room ───────────────────────────────────────────────
        Booking booking = hms.bookingService.bookRoom("alice@example.com", "R102", checkIn, checkOut);
        System.out.println("\nBooked: " + booking);
        hms.notificationService.sendBookingConfirmation(alice, booking);

        // ─── 6. Same room — conflict should throw ─────────────────────────
        try {
            hms.bookingService.bookRoom("bob@example.com", "R102",
                    LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7));
        } catch (hotel.exception.BookingConflictException e) {
            System.out.println("\nExpected conflict: " + e.getMessage());
        }

        // ─── 7. Process payment ───────────────────────────────────────────
        Payment payment = hms.paymentService.processPayment(booking.getBookingId(), PaymentMethod.CREDIT_CARD);
        hms.notificationService.sendPaymentReceipt(alice, payment);

        // ─── 8. Check-in ──────────────────────────────────────────────────
        hms.bookingService.checkIn(booking.getBookingId());
        System.out.println("\nChecked in. Room status: " + hms.hotelService.getRoom("R102").getStatus());

        // ─── 9. Check-out ─────────────────────────────────────────────────
        hms.bookingService.checkOut(booking.getBookingId());
        System.out.println("Checked out. Room status: " + hms.hotelService.getRoom("R102").getStatus());
        System.out.println("Booking status: " + booking.getStatus());

        // ─── 10. Weekend pricing demo ─────────────────────────────────────
        hms.setPricingStrategy(new WeekendPricingStrategy());
        LocalDate fri = LocalDate.of(2026, 10, 9);   // Friday
        LocalDate sun = LocalDate.of(2026, 10, 11);  // Sunday (2 nights)
        Booking weekendBooking = hms.bookingService.bookRoom("bob@example.com", "R102", fri, sun);
        System.out.println("\nWeekend booking: " + weekendBooking);
        System.out.println("Weekend price (2 nights × 4000 × 1.2): $" + weekendBooking.getTotalPrice());

        // ─── 11. Maintenance ──────────────────────────────────────────────
        hms.hotelService.getRoom("R201").markMaintenance();
        System.out.println("\nSuite R201 status: " + hms.hotelService.getRoom("R201").getStatus());

        System.out.println("\n=== All done ===");
    }
}

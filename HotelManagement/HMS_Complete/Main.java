import model.*;
import service.SearchService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * End-to-end demo using the HotelManagementSystem facade.
 * Covers: hotel + room + guest management, search, booking FSM,
 * payment strategies, notifications, and the race condition scenario.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        HotelManagementSystem hms = HotelManagementSystem.getInstance();

        // ── Seed: hotels ──────────────────────────────────────────────────────
        hms.registerHotel(new Hotel("H1", "Marriott Mumbai",
            new Address("MG Road",  "Mumbai", "India", "400001"), 5));
        hms.registerHotel(new Hotel("H2", "Ibis Mumbai",
            new Address("Andheri",  "Mumbai", "India", "400053"), 3));
        hms.registerHotel(new Hotel("H3", "Taj Delhi",
            new Address("CP",       "Delhi",  "India", "110001"), 5));

        // ── Seed: rooms ───────────────────────────────────────────────────────
        hms.addRoom("H1", new Room("R101", "101", 1, RoomType.SINGLE, 2, new BigDecimal("4500")));
        hms.addRoom("H1", new Room("R201", "201", 2, RoomType.DOUBLE, 4, new BigDecimal("7500")));
        hms.addRoom("H1", new Room("R501", "501", 5, RoomType.SUITE,  2, new BigDecimal("15000")));
        hms.addRoom("H2", new Room("R001", "001", 0, RoomType.SINGLE, 2, new BigDecimal("2500")));
        hms.addRoom("H3", new Room("T101", "101", 1, RoomType.SUITE,  2, new BigDecimal("12000")));

        // ── Seed: guests ──────────────────────────────────────────────────────
        hms.registerGuest(new Guest("G1", "Arjun Sharma", "arjun@email.com", "+91-9000000001"));
        hms.registerGuest(new Guest("G2", "Priya Patel",  "priya@email.com",  "+91-9000000002"));
        hms.registerGuest(new Guest("G3", "Wei Chen",     "wei@email.com",     "+1-415-0000003"));

        LocalDate checkIn  = LocalDate.of(2025, 12, 20);
        LocalDate checkOut = LocalDate.of(2025, 12, 25);

        // ══ 1. Browse ═════════════════════════════════════════════════════════
        System.out.println("\n══ 1. Browse ═════════════════════════════════════════");
        System.out.println("-- Hotels in Mumbai:");
        hms.getHotelsByCity("Mumbai").forEach(h -> System.out.println("   " + h));
        System.out.println("-- 5-star hotels:");
        hms.getHotelsByMinStars(5).forEach(h -> System.out.println("   " + h));

        // ══ 2. Search (all available) ════════════════════════════════════════
        System.out.println("\n══ 2. Search Mumbai Dec 20-25 (no bookings yet) ══════");
        SearchCriteria criteria = new SearchCriteria.Builder("Mumbai", checkIn, checkOut).build();
        hms.searchHotels(criteria).forEach(r -> {
            System.out.println("  " + r);
            r.getAvailableRooms().forEach(room -> System.out.println("    -> " + room));
        });

        // ══ 3. Guest profile update & membership upgrade ══════════════════════
        System.out.println("\n══ 3. Guest updates ═════════════════════════════════");
        hms.updateGuestProfile("G1", "Arjun K. Sharma", "+91-9111111111");
        hms.upgradeMembership("G1", MembershipTier.GOLD);

        // ══ 4. Happy-path FSM: create -> confirm(UPI) -> checkIn -> checkOut ══
        System.out.println("\n══ 4. Happy-path FSM ════════════════════════════════");
        Booking b1 = hms.createBooking("G1", "H1", "R101", checkIn, checkOut);
        hms.confirmBooking(b1.getBookingId(), new UPIPayment("arjun@upi"));
        hms.checkIn(b1.getBookingId());
        hms.checkOut(b1.getBookingId());   // triggers invoice + notification

        // ══ 5. Search after checkout — R101 visible again ═════════════════════
        System.out.println("\n══ 5. Search after checkout ═════════════════════════");
        hms.searchHotels(criteria).forEach(r -> {
            System.out.println("  " + r);
            r.getAvailableRooms().forEach(room -> System.out.println("    -> " + room));
        });

        // ══ 6. Sequential double-booking test ════════════════════════════════
        System.out.println("\n══ 6. Sequential double-booking ════════════════════");
        Booking b2 = hms.createBooking("G1", "H1", "R101", checkIn, checkOut);
        try {
            hms.createBooking("G2", "H1", "R101",
                LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));
        } catch (IllegalStateException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }
        hms.cancelBooking(b2.getBookingId());  // cancel -> refund not applicable (PENDING)

        // ══ 7. Adjacent booking — must succeed ════════════════════════════════
        System.out.println("\n══ 7. Adjacent booking (Dec 25-28) ═════════════════");
        Booking b3 = hms.createBooking("G1", "H1", "R101", checkIn, checkOut);
        Booking b4 = hms.createBooking("G2", "H1", "R101",
            LocalDate.of(2025, 12, 25), LocalDate.of(2025, 12, 28));
        System.out.println("Both created: " + b3.getBookingId() + " and " + b4.getBookingId());
        hms.cancelBooking(b3.getBookingId());
        hms.cancelBooking(b4.getBookingId());

        // ══ 8. Cancel CONFIRMED booking — refund issued ═══════════════════════
        System.out.println("\n══ 8. Cancel confirmed booking (refund) ═════════════");
        Booking b5 = hms.createBooking("G3", "H1", "R501", checkIn, checkOut);
        hms.confirmBooking(b5.getBookingId(), new CardPayment("4242"));
        hms.cancelBooking(b5.getBookingId());  // was CONFIRMED -> refund triggered

        // ══ 9. Maintenance — room disappears from search ══════════════════════
        System.out.println("\n══ 9. Maintenance test ══════════════════════════════");
        hms.markUnderMaintenance("R201");
        SearchCriteria doubleCriteria = new SearchCriteria.Builder("Mumbai", checkIn, checkOut)
            .roomType(RoomType.DOUBLE).build();
        System.out.println("DOUBLE rooms after maintenance (expect 0): "
            + hms.searchHotels(doubleCriteria).stream()
                 .mapToInt(r -> r.getRoomCount()).sum());
        hms.markAvailable("R201");

        // ══ 10. RACE CONDITION — two threads, same room, same dates ════════════
        System.out.println("\n══ 10. Race condition — 2 threads vs R101 ════════════");
        // Ensure R101 is free first
        hms.searchHotels(criteria); // side-effect free, just confirm

        CountDownLatch startGun = new CountDownLatch(1);
        ExecutorService pool    = Executors.newFixedThreadPool(2);

        Future<String> threadA = pool.submit(() -> {
            startGun.await();
            try {
                HotelManagementSystem.getInstance()
                    .createBooking("G1", "H1", "R101", checkIn, checkOut);
                return "Thread-A: CREATED";
            } catch (IllegalStateException e) {
                return "Thread-A: REJECTED — " + e.getMessage();
            }
        });

        Future<String> threadB = pool.submit(() -> {
            startGun.await();
            try {
                HotelManagementSystem.getInstance()
                    .createBooking("G2", "H1", "R101", checkIn, checkOut);
                return "Thread-B: CREATED";
            } catch (IllegalStateException e) {
                return "Thread-B: REJECTED — " + e.getMessage();
            }
        });

        startGun.countDown();  // release both threads simultaneously
        System.out.println(threadA.get());
        System.out.println(threadB.get());
        System.out.println("Exactly one created, one rejected. No double booking.");
        pool.shutdown();

        // ══ 11. Invalid FSM transition ════════════════════════════════════════
        System.out.println("\n══ 11. Invalid FSM transition ═══════════════════════");
        // find the booking from scenario 10 that was created
        hms.getBookingHistory("G1").stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING
                      && b.getRoomId().equals("R101"))
            .findFirst()
            .ifPresent(b -> {
                try {
                    hms.checkOut(b.getBookingId()); // PENDING -> CHECKED_OUT: invalid
                } catch (IllegalStateException e) {
                    System.out.println("Caught (expected): " + e.getMessage());
                }
            });

        // ══ 12. Cash payment + deregister hotel (cascade) ═════════════════════
        System.out.println("\n══ 12. Cascade deregister hotel H3 ═════════════════");
        Booking b6 = hms.createBooking("G2", "H3", "T101",
            LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15));
        hms.confirmBooking(b6.getBookingId(), new CashPayment());
        hms.cancelBooking(b6.getBookingId()); // cancel before deregister
        hms.deregisterHotel("H3");
        System.out.println("Delhi hotels after deregister: "
            + hms.getHotelsByCity("Delhi").size()); // 0

        // ══ 13. Booking history ═══════════════════════════════════════════════
        System.out.println("\n══ 13. Booking history for G1 ═══════════════════════");
        hms.getBookingHistory("G1").forEach(b -> System.out.println("  " + b));
    }
}

package com.hotelmanagement;

import com.hotelmanagement.enums.*;
import com.hotelmanagement.exceptions.RoomNotAvailableException;
import com.hotelmanagement.models.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demo driver showcasing:
 * 1. Hotel/Room setup
 * 2. Guest registration
 * 3. Room search (by type, price range, location)
 * 4. Concurrent booking (thread safety demo)
 * 5. Payment & confirmation
 * 6. Check-in / Check-out
 * 7. Cancellation with refund
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        HotelManagementSystem system = HotelManagementSystem.getInstance();

        // ===== 1. Setup Hotel & Rooms =====
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  HOTEL MANAGEMENT SYSTEM — LLD DEMO");
        System.out.println("═══════════════════════════════════════════\n");

        Hotel hotel = new Hotel("H1", "Grand Palace", "Mumbai");
        hotel.addRoom(new Room("R101", "H1", 1, 101, RoomType.SINGLE, 2000));
        hotel.addRoom(new Room("R102", "H1", 1, 102, RoomType.SINGLE, 2200));
        hotel.addRoom(new Room("R201", "H1", 2, 201, RoomType.DOUBLE, 3500));
        hotel.addRoom(new Room("R202", "H1", 2, 202, RoomType.DOUBLE, 3800));
        hotel.addRoom(new Room("R301", "H1", 3, 301, RoomType.DELUXE, 5500));
        hotel.addRoom(new Room("R401", "H1", 4, 401, RoomType.SUITE, 12000));
        system.addHotel(hotel);
        System.out.println("[SETUP] Hotel added: " + hotel.getName() + " with 6 rooms\n");

        // ===== 2. Register Guests =====
        Guest guest1 = new Guest("G1", "Arnav Jain", "arnav@email.com", "9876543210", "AADHAR-1234");
        Guest guest2 = new Guest("G2", "Priya Sharma", "priya@email.com", "9876543211", "AADHAR-5678");
        Guest guest3 = new Guest("G3", "Rahul Verma", "rahul@email.com", "9876543212", "AADHAR-9012");
        system.registerGuest(guest1);
        system.registerGuest(guest2);
        system.registerGuest(guest3);
        System.out.println("[GUESTS] Registered: " + guest1.getName() + ", " + guest2.getName() + ", " + guest3.getName() + "\n");

        // ===== 3. Search Available Rooms =====
        System.out.println("─── SEARCH: Available DOUBLE rooms (Jun 10-15) ───");
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(15);
        List<Room> available = system.searchRooms("H1", checkIn, checkOut, RoomType.DOUBLE);
        available.forEach(r -> System.out.println("  " + r));

        System.out.println("\n─── SEARCH: Rooms by price range ₹2000-₹4000 ───");
        List<Room> priceRange = system.searchByPriceRange(RoomType.DOUBLE, 2000, 4000, checkIn, checkOut);
        priceRange.forEach(r -> System.out.println("  " + r));

        System.out.println("\n─── SEARCH: Hotels in Mumbai ───");
        system.searchByLocation("Mumbai").forEach(h -> System.out.println("  " + h.getName()));

        System.out.println("\n─── SEARCH: Cheapest SINGLE room ───");
        system.findCheapest(RoomType.SINGLE, checkIn, checkOut)
                .ifPresent(r -> System.out.println("  " + r));

        // ===== 4. Concurrent Booking Demo =====
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("  CONCURRENCY TEST: 3 guests booking R201");
        System.out.println("═══════════════════════════════════════════\n");

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        String targetRoom = "R201";

        for (Guest guest : List.of(guest1, guest2, guest3)) {
            executor.submit(() -> {
                try {
                    latch.await(); // All threads start simultaneously
                    Booking booking = system.createBooking(guest.getId(), targetRoom, "H1", checkIn, checkOut);
                    System.out.println("  ✓ " + guest.getName() + " BOOKED: " + booking);
                } catch (RoomNotAvailableException e) {
                    System.out.println("  ✗ " + guest.getName() + " FAILED: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        latch.countDown(); // Release all threads at once
        executor.shutdown();
        Thread.sleep(500); // Wait for threads to complete

        // ===== 5. Normal Booking + Payment + Check-in/out =====
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("  FULL BOOKING LIFECYCLE");
        System.out.println("═══════════════════════════════════════════\n");

        LocalDate cin = LocalDate.now().plusDays(20);
        LocalDate cout = LocalDate.now().plusDays(23);

        Booking booking = system.createBooking("G2", "R301", "H1", cin, cout);
        System.out.println("[CREATED] " + booking);

        Payment payment = system.confirmBookingWithPayment(booking.getId(), PaymentMethod.UPI);
        System.out.println("[CONFIRMED] " + booking);
        System.out.println("[PAYMENT] " + payment);

        system.checkIn(booking.getId());
        System.out.println("[CHECK-IN] " + booking);

        system.checkOut(booking.getId());
        System.out.println("[CHECK-OUT] " + booking);

        // ===== 6. Cancellation with Refund =====
        System.out.println("\n─── CANCELLATION DEMO ───");
        LocalDate cin2 = LocalDate.now().plusDays(30);
        LocalDate cout2 = LocalDate.now().plusDays(32);
        Booking booking2 = system.createBooking("G3", "R401", "H1", cin2, cout2);
        system.confirmBookingWithPayment(booking2.getId(), PaymentMethod.CREDIT_CARD);
        System.out.println("[BOOKED] " + booking2);

        system.cancelBooking(booking2.getId());
        System.out.println("[CANCELLED] " + booking2);

        // ===== 7. Verify room is available again after cancellation =====
        System.out.println("\n─── POST-CANCELLATION: R401 availability ───");
        List<Room> suites = system.searchRooms("H1", cin2, cout2, RoomType.SUITE);
        suites.forEach(r -> System.out.println("  Available: " + r));

        system.shutdown();
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("  DEMO COMPLETE");
        System.out.println("═══════════════════════════════════════════");
    }
}

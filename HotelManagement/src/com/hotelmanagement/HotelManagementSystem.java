package com.hotelmanagement;

import com.hotelmanagement.enums.PaymentMethod;
import com.hotelmanagement.enums.RoomType;
import com.hotelmanagement.models.*;
import com.hotelmanagement.notification.EmailNotificationService;
import com.hotelmanagement.notification.NotificationService;
import com.hotelmanagement.payment.DefaultPaymentService;
import com.hotelmanagement.payment.PaymentService;
import com.hotelmanagement.service.BookingManager;
import com.hotelmanagement.service.RoomManager;
import com.hotelmanagement.service.SearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton facade — single entry point for all hotel management operations.
 * Thread-safe initialization via double-checked locking.
 */
public class HotelManagementSystem {
    private static volatile HotelManagementSystem instance;

    private final RoomManager roomManager;
    private final BookingManager bookingManager;
    private final SearchService searchService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final ConcurrentHashMap<String, Guest> guestRegistry; // guestId -> Guest

    private HotelManagementSystem() {
        this.roomManager = new RoomManager();
        this.bookingManager = new BookingManager(roomManager);
        this.searchService = new SearchService(roomManager);
        this.paymentService = new DefaultPaymentService();
        this.notificationService = new EmailNotificationService();
        this.guestRegistry = new ConcurrentHashMap<>();
    }

    public static HotelManagementSystem getInstance() {
        if (instance == null) {
            synchronized (HotelManagementSystem.class) {
                if (instance == null) {
                    instance = new HotelManagementSystem();
                }
            }
        }
        return instance;
    }

    // ===== Hotel & Room Management =====
    public void addHotel(Hotel hotel) {
        roomManager.addHotel(hotel);
        searchService.indexHotel(hotel);
    }

    // ===== Guest Management =====
    public void registerGuest(Guest guest) { guestRegistry.put(guest.getId(), guest); }
    public Guest getGuest(String guestId) { return guestRegistry.get(guestId); }

    // ===== Search =====
    public List<Room> searchRooms(String hotelId, LocalDate checkIn, LocalDate checkOut, RoomType type) {
        return searchService.searchAvailableRooms(hotelId, checkIn, checkOut, type);
    }

    public List<Room> searchByPriceRange(RoomType type, double min, double max,
                                         LocalDate checkIn, LocalDate checkOut) {
        return searchService.searchByTypeAndPriceRange(type, min, max, checkIn, checkOut);
    }

    public List<Hotel> searchByLocation(String location) {
        return searchService.searchByLocation(location);
    }

    public Optional<Room> findCheapest(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        return searchService.findCheapestAvailable(type, checkIn, checkOut);
    }

    // ===== Booking Lifecycle =====
    public Booking createBooking(String guestId, String roomId, String hotelId,
                                 LocalDate checkIn, LocalDate checkOut) {
        Booking booking = bookingManager.createBooking(guestId, roomId, hotelId, checkIn, checkOut);
        // Calculate and set total
        Room room = roomManager.getRoom(roomId);
        double total = paymentService.calculateTotal(booking, room.getBasePricePerNight());
        booking.setTotalAmount(total);
        // Track in guest history
        Guest guest = guestRegistry.get(guestId);
        if (guest != null) guest.addBooking(booking.getId());
        return booking;
    }

    public Payment confirmBookingWithPayment(String bookingId, PaymentMethod method) {
        Booking booking = bookingManager.getBooking(bookingId);
        Payment payment = paymentService.processPayment(booking, method);
        bookingManager.confirmBooking(bookingId);
        Guest guest = guestRegistry.get(booking.getGuestId());
        if (guest != null) notificationService.notifyBookingConfirmed(booking, guest);
        return payment;
    }

    public void cancelBooking(String bookingId) {
        Booking booking = bookingManager.getBooking(bookingId);
        bookingManager.cancelBooking(bookingId);
        Guest guest = guestRegistry.get(booking.getGuestId());
        if (guest != null) notificationService.notifyCancellation(booking, guest);
        // Process refund
        paymentService.processRefund(booking);
    }

    public void checkIn(String bookingId) {
        bookingManager.checkIn(bookingId);
        Booking booking = bookingManager.getBooking(bookingId);
        Guest guest = guestRegistry.get(booking.getGuestId());
        if (guest != null) notificationService.notifyCheckIn(booking, guest);
    }

    public void checkOut(String bookingId) {
        bookingManager.checkOut(bookingId);
        Booking booking = bookingManager.getBooking(bookingId);
        Guest guest = guestRegistry.get(booking.getGuestId());
        if (guest != null) notificationService.notifyCheckOut(booking, guest);
    }

    public void shutdown() { bookingManager.shutdown(); }
}

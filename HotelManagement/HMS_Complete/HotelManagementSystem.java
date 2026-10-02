import model.*;
import repository.*;
import service.*;

import java.time.LocalDate;
import java.util.List;

/**
 * HotelManagementSystem — Facade + Singleton.
 *
 * Facade   : single entry point for all HMS operations. Hides the complexity
 *            of repositories, services, and their wiring from callers.
 *            Coordinates cross-service operations (e.g. deregisterHotel
 *            removes rooms first, then the hotel).
 *
 * Singleton: only one HMS instance per JVM. Uses double-checked locking
 *            with a volatile field to be safe under Java memory model.
 *
 *            volatile prevents instruction reordering — without it, another
 *            thread could see a partially constructed instance.
 */
public class HotelManagementSystem {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile HotelManagementSystem instance;

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

    // ── Services (wired in private constructor) ───────────────────────────────
    private final HotelService       hotelService;
    private final RoomService        roomService;
    private final GuestService       guestService;
    private final SearchService      searchService;
    private final BookingService     bookingService;
    private final PaymentService     paymentService;
    private final NotificationService notificationService;

    private HotelManagementSystem() {
        // repositories
        HotelRepository   hotelRepo   = new HotelRepository();
        RoomRepository    roomRepo    = new RoomRepository();
        GuestRepository   guestRepo   = new GuestRepository();
        BookingRepository bookingRepo = new BookingRepository();

        // leaf services (no inter-service dependencies)
        this.paymentService      = new PaymentService();
        this.notificationService = new NotificationService(
            List.of(new EmailChannel(), new SMSChannel()));

        // mid-tier services
        this.hotelService  = new HotelService(hotelRepo);
        this.roomService   = new RoomService(roomRepo, hotelRepo);
        this.guestService  = new GuestService(guestRepo);
        this.searchService = new SearchService(hotelRepo, roomRepo, bookingRepo);

        // booking service depends on payment + notification
        this.bookingService = new BookingService(
            bookingRepo, roomRepo, guestRepo,
            paymentService, notificationService);
    }

    // ── Hotel Admin ───────────────────────────────────────────────────────────

    public void registerHotel(Hotel hotel) {
        hotelService.registerHotel(hotel);
    }

    /** Cascade deregister: rooms removed first, then hotel. */
    public void deregisterHotel(String hotelId) {
        roomService.removeAllRoomsForHotel(hotelId);
        hotelService.deregisterHotel(hotelId);
    }

    public void addRoom(String hotelId, Room room) {
        roomService.addRoom(hotelId, room);
    }

    public void removeRoom(String roomId) {
        roomService.removeRoom(roomId);
    }

    public void markUnderMaintenance(String roomId) {
        roomService.markUnderMaintenance(roomId);
    }

    public void markAvailable(String roomId) {
        roomService.markAvailable(roomId);
    }

    // ── Guest ─────────────────────────────────────────────────────────────────

    public void registerGuest(Guest guest) {
        guestService.registerGuest(guest);
    }

    public void updateGuestProfile(String guestId, String name, String phone) {
        guestService.updateProfile(guestId, name, phone);
    }

    public void upgradeMembership(String guestId, MembershipTier tier) {
        guestService.upgradeMembership(guestId, tier);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<Hotel> getHotelsByCity(String city) {
        return searchService.getHotelsByCity(city);
    }

    public List<Hotel> getHotelsByMinStars(int minStars) {
        return searchService.getHotelsByMinStars(minStars);
    }

    public List<SearchResult> searchHotels(SearchCriteria criteria) {
        return searchService.searchHotels(criteria);
    }

    public List<Room> searchRoomsInHotel(String hotelId, SearchCriteria criteria) {
        return searchService.searchRoomsInHotel(hotelId, criteria);
    }

    // ── Booking ───────────────────────────────────────────────────────────────

    public Booking createBooking(String guestId, String hotelId, String roomId,
                                  LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createBooking(guestId, hotelId, roomId, checkIn, checkOut);
    }

    public Booking confirmBooking(String bookingId, PaymentMethod method) {
        return bookingService.confirmBooking(bookingId, method);
    }

    public Booking checkIn(String bookingId) {
        return bookingService.checkIn(bookingId);
    }

    public Booking checkOut(String bookingId) {
        return bookingService.checkOut(bookingId);
    }

    public Booking cancelBooking(String bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    public List<Booking> getBookingHistory(String guestId) {
        return bookingService.getBookingHistory(guestId);
    }
}

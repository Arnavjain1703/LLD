package hotel;

import hotel.notification.NotificationService;
import hotel.pricing.PricingStrategy;
import hotel.repository.*;
import hotel.search.SearchCriteria;
import hotel.search.SearchResult;
import hotel.search.SearchService;
import hotel.service.*;

import java.util.List;

/**
 * Facade singleton — single entry point for the hotel management system.
 * Uses double-checked locking with volatile to prevent instruction reordering.
 */
public class HotelManagementSystem {

    private static volatile HotelManagementSystem instance;

    // repositories
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // services
    public final HotelService hotelService;
    public final GuestService guestService;
    public final BookingService bookingService;
    public final PaymentService paymentService;
    public final SearchService searchService;
    public final NotificationService notificationService;

    private HotelManagementSystem() {
        hotelRepository   = new InMemoryHotelRepository();
        roomRepository    = new InMemoryRoomRepository();
        guestRepository   = new InMemoryGuestRepository();
        bookingRepository = new InMemoryBookingRepository();
        paymentRepository = new InMemoryPaymentRepository();

        hotelService   = new HotelService(hotelRepository, roomRepository);
        guestService   = new GuestService(guestRepository);
        bookingService = new BookingService(bookingRepository, hotelService, guestService);
        paymentService = new PaymentService(paymentRepository, bookingService);
        searchService  = new SearchService(hotelRepository, roomRepository, bookingRepository);
        notificationService = new NotificationService();
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

    /** Convenience: swap pricing strategy globally. */
    public void setPricingStrategy(PricingStrategy strategy) {
        bookingService.setPricingStrategy(strategy);
    }

    public List<SearchResult> search(SearchCriteria criteria) {
        return searchService.search(criteria);
    }
}

package library;

import library.fine.FineStrategyFactory;
import library.notification.NotificationDispatcher;
import library.repository.BookItemRepository;
import library.repository.BookLendingRepository;
import library.repository.BookRepository;
import library.repository.FineRepository;
import library.repository.InMemoryBookItemRepository;
import library.repository.InMemoryBookLendingRepository;
import library.repository.InMemoryBookRepository;
import library.repository.InMemoryFineRepository;
import library.repository.InMemoryMemberRepository;
import library.repository.MemberRepository;
import library.reservation.ReservationQueue;
import library.search.CatalogSearchService;
import library.service.BookService;
import library.service.BorrowService;
import library.service.FineService;
import library.service.MemberService;
import library.service.ReservationService;
import library.service.ReturnService;

/**
 * Library — singleton entry point.
 *
 * Owns and creates exactly one instance of every repository, service,
 * and shared component. All other classes are plain objects — the singleton
 * guarantee lives here, not in each individual class.
 *
 * Double-checked locking + volatile ensures safe publication across threads
 * without locking on every call after initialisation.
 */
public class Library {

    private static volatile Library instance;

    // ── services ──────────────────────────────────────────────────────────────
    private final BookService        bookService;
    private final MemberService      memberService;
    private final BorrowService      borrowService;
    private final ReturnService      returnService;
    private final ReservationService reservationService;
    private final FineService        fineService;
    private final NotificationDispatcher notificationDispatcher;

    // ── private constructor — wires the whole graph ───────────────────────────
    private Library() {

        // shared infrastructure — created once, injected everywhere needed
        NotificationDispatcher dispatcher    = new NotificationDispatcher();
        ReservationQueue       reservQueue   = new ReservationQueue();
        FineStrategyFactory    fineFactory   = new FineStrategyFactory();

        // repositories — each holds a single in-memory map; must not be duplicated
        BookRepository        bookRepo     = new InMemoryBookRepository();
        BookItemRepository    bookItemRepo = new InMemoryBookItemRepository();
        MemberRepository      memberRepo   = new InMemoryMemberRepository();
        BookLendingRepository lendingRepo  = new InMemoryBookLendingRepository();
        FineRepository        fineRepo     = new InMemoryFineRepository();

        // catalog search index — shares bookItemRepo to check availability
        CatalogSearchService catalogSearch = new CatalogSearchService(bookItemRepo);

        // services — wired with their dependencies
        this.bookService     = new BookService(bookRepo, bookItemRepo, catalogSearch);
        this.memberService   = new MemberService(memberRepo);
        this.fineService     = new FineService(fineRepo, fineFactory);

        // reservationService needs bookRepo + dispatcher + queue
        this.reservationService = new ReservationService(reservQueue, dispatcher, bookRepo);

        // borrowService needs bookItemRepo + lendingRepo + dispatcher
        this.borrowService   = new BorrowService(bookItemRepo, lendingRepo, dispatcher);

        // returnService needs lendingRepo + fineService + reservationService + dispatcher
        this.returnService   = new ReturnService(lendingRepo, fineService, reservationService, dispatcher);

        this.notificationDispatcher = dispatcher;
    }

    // ── double-checked locking — volatile prevents instruction reordering ─────
    public static Library getInstance() {
        if (instance == null) {
            synchronized (Library.class) {
                if (instance == null) {
                    instance = new Library();
                }
            }
        }
        return instance;
    }

    // ── accessors ─────────────────────────────────────────────────────────────
    public BookService        getBookService()        { return bookService; }
    public MemberService      getMemberService()      { return memberService; }
    public BorrowService      getBorrowService()      { return borrowService; }
    public ReturnService      getReturnService()      { return returnService; }
    public ReservationService getReservationService() { return reservationService; }
    public FineService        getFineService()        { return fineService; }
    public NotificationDispatcher getNotificationDispatcher() { return notificationDispatcher; }
}

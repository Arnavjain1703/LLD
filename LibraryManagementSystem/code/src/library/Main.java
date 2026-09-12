package library;

import library.exception.DuplicateMemberException;
import library.model.Book;
import library.model.BookItem;
import library.model.BookLending;
import library.model.BookReservation;
import library.model.Fine;
import library.model.Member;
import library.model.MemberTier;
import library.search.SearchCriteria;
import library.service.BookService;
import library.service.BorrowService;
import library.service.FineService;
import library.service.MemberService;
import library.service.ReservationService;
import library.service.ReturnService;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        // ── single entry point — all services come from here ─────────────────
        Library lib = Library.getInstance();

        BookService        bookService        = lib.getBookService();
        MemberService      memberService      = lib.getMemberService();
        BorrowService      borrowService      = lib.getBorrowService();
        ReturnService      returnService      = lib.getReturnService();
        ReservationService reservationService = lib.getReservationService();
        FineService        fineService        = lib.getFineService();

        // ═══════════════════════════════════════════════════════════════════
        // Step 1 — BookService
        // ═══════════════════════════════════════════════════════════════════

        System.out.println("\n--- BookService ---");

        Book ddia = bookService.addBook(new Book(
                "978-1449373320", "Designing Data-Intensive Applications",
                List.of("Martin Kleppmann"), "Technology", "O'Reilly", "English", "1st"));

        Book clean = bookService.addBook(new Book(
                "978-0132350884", "Clean Code",
                List.of("Robert C. Martin"), "Technology", "Prentice Hall", "English", "1st"));

        Book hobbit = bookService.addBook(new Book(
                "978-0547928227", "The Hobbit",
                List.of("J.R.R. Tolkien"), "Fantasy", "Houghton Mifflin", "English", "1st"));

        // add copies
        bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        bookService.addBookItem(clean.getIsbn(), new BookItem(clean));

        System.out.println("All books: " + bookService.getAllBooks());

        // search by genre
        System.out.println("Technology books: " + bookService.search(
                SearchCriteria.builder().genre("Technology").build()));

        // delete
        bookService.deleteBook(hobbit.getIsbn());
        System.out.println("After deleting Hobbit: " + bookService.getAllBooks());

        // ═══════════════════════════════════════════════════════════════════
        // Step 2 — MemberService
        // ═══════════════════════════════════════════════════════════════════

        System.out.println("\n--- MemberService ---");

        Member alice = memberService.register(new Member("Alice", "alice@lib.com", "9001"));
        Member carol = memberService.register(new Member("Carol", "carol@lib.com", "9003"));
        Member dave  = memberService.register(new Member("Dave",  "dave@lib.com",  "9004"));

        // duplicate email blocked
        try {
            memberService.register(new Member("Alice2", "alice@lib.com", "9999"));
        } catch (DuplicateMemberException e) {
            System.out.println("Duplicate blocked: " + e.getMessage());
        }

        // suspend / reactivate
        memberService.suspend("alice@lib.com");
        System.out.println("Alice canBorrow (suspended): " + alice.canBorrow());
        memberService.reactivate("alice@lib.com");
        System.out.println("Alice canBorrow (reactivated): " + alice.canBorrow());

        // upgrade Carol to LIBRARIAN
        memberService.upgradeTier("carol@lib.com", MemberTier.LIBRARIAN);
        System.out.println("Carol isLibrarian: " + carol.isLibrarian());

        // ═══════════════════════════════════════════════════════════════════
        // Step 3 — BorrowService
        // ═══════════════════════════════════════════════════════════════════

        System.out.println("\n--- BorrowService ---");

        BookLending lending1 = borrowService.borrowBook(alice, ddia.getIsbn());
        System.out.println("Alice borrowed: " + lending1);
        System.out.println("Copy status: " + lending1.getBookItem().getStatus());

        BookLending lending2 = borrowService.borrowBook(alice, clean.getIsbn());
        System.out.println("Alice borrowed second book: " + lending2);

        // suspended member cannot borrow
        memberService.suspend("dave@lib.com");
        try {
            borrowService.borrowBook(dave, ddia.getIsbn());
        } catch (Exception e) {
            System.out.println("Suspended member blocked: " + e.getMessage());
        }
        memberService.reactivate("dave@lib.com");

        // ═══════════════════════════════════════════════════════════════════
        // Step 5 — ReservationService (before Return, to set up queue)
        // ═══════════════════════════════════════════════════════════════════

        System.out.println("\n--- ReservationService ---");

        // DDIA has 2 copies — both borrowed by alice + dave
        bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        BookLending daveLending = borrowService.borrowBook(dave, ddia.getIsbn());

        // Carol reserves DDIA — all copies out
        BookReservation carolRes = reservationService.reserve(carol, ddia.getIsbn());
        System.out.println("Carol reservation: " + carolRes);
        System.out.println("Queue has waiting: " + reservationService.hasWaiting(ddia.getIsbn()));

        // ═══════════════════════════════════════════════════════════════════
        // Step 4 — ReturnService
        // ═══════════════════════════════════════════════════════════════════

        System.out.println("\n--- ReturnService ---");

        // Alice returns Clean Code on time — no fine
        Fine f1 = returnService.returnBook(alice, lending2.getBookItem().getBarcode());
        System.out.println("On-time return fine: " + f1);
        System.out.println("Clean Code copy status: " + lending2.getBookItem().getStatus());

        // Alice returns DDIA — Carol is waiting, copy goes RESERVED
        Fine f2 = returnService.returnBook(alice, lending1.getBookItem().getBarcode());
        System.out.println("DDIA copy status after return (Carol waiting): "
                + lending1.getBookItem().getStatus());
        System.out.println("Carol reservation status: " + carolRes.getStatus());

        // ═══════════════════════════════════════════════════════════════════
        // Step 6 — FineService
        // ═══════════════════════════════════════════════════════════════════

        System.out.println("\n--- FineService ---");

        // Dave still has DDIA — check projected fine (0 since just borrowed)
        List<BookLending> daveActive = lib.getBorrowService() == borrowService
                ? List.of(daveLending) : List.of();
        System.out.println("Dave projected fine (just borrowed): $"
                + String.format("%.2f", fineService.getProjectedFine(daveLending)));

        // Simulate Dave returning overdue: set returnDate = dueDate + 7 days
        daveLending.setReturnDate(daveLending.getDueDate().plusDays(7));
        Fine daveFine = fineService.payFineForLending(daveLending);
        System.out.println("Dave fine after 7 overdue days ($1/day): " + daveFine);

        // Carol is LIBRARIAN — waived strategy
        BookLending carolLending = borrowService.borrowBook(carol, clean.getIsbn());
        carolLending.setReturnDate(carolLending.getDueDate().plusDays(5));
        Fine carolFine = fineService.payFineForLending(carolLending);
        System.out.println("Carol (LIBRARIAN) fine: " + carolFine);  // null

        // Payment history
        System.out.println("Dave fine history: " + fineService.getFineHistory("dave@lib.com"));

        // Waive a fine via librarian
        bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        BookLending aliceLending2 = borrowService.borrowBook(alice, ddia.getIsbn());
        aliceLending2.setReturnDate(aliceLending2.getDueDate().plusDays(3));
        Fine aliceFine = fineService.waiveFineForLending(aliceLending2, "carol@lib.com");
        System.out.println("Alice fine waived by Carol: " + aliceFine);
    }
}

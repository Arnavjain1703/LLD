package library;

import library.exception.DuplicateMemberException;
import library.model.Book;
import library.model.BookItem;
import library.model.Member;
import library.model.MemberTier;
import library.repository.BookItemRepository;
import library.repository.BookRepository;
import library.repository.InMemoryBookItemRepository;
import library.repository.InMemoryBookRepository;
import library.repository.InMemoryMemberRepository;
import library.repository.MemberRepository;
import library.search.CatalogSearchService;
import library.search.SearchCriteria;
import library.service.BookService;
import library.service.MemberService;
import library.model.BookLending;
import library.notification.NotificationDispatcher;
import library.repository.BookLendingRepository;
import library.repository.InMemoryBookLendingRepository;
import library.service.BorrowService;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        // ═══════════════════════════════════════════════════════════════════
        // Step 1 — BookService
        // ═══════════════════════════════════════════════════════════════════

        BookRepository bookRepository         = new InMemoryBookRepository();
        BookItemRepository bookItemRepository = new InMemoryBookItemRepository();
        CatalogSearchService catalogSearch    = new CatalogSearchService(bookItemRepository);
        BookService bookService               = new BookService(bookRepository, bookItemRepository, catalogSearch);

        // ── Add books ───────────────────────────────────────────────────────
        Book ddia = bookService.addBook(new Book(
            "978-1449373320", "Designing Data-Intensive Applications",
            List.of("Martin Kleppmann"), "Technology", "O'Reilly", "English", "1st"));

        Book clean = bookService.addBook(new Book(
            "978-0132350884", "Clean Code",
            List.of("Robert C. Martin"), "Technology", "Prentice Hall", "English", "1st"));

        Book hobbit = bookService.addBook(new Book(
            "978-0547928227", "The Hobbit",
            List.of("J.R.R. Tolkien"), "Fantasy", "Houghton Mifflin", "English", "1st"));

        System.out.println("All books: " + bookService.getAllBooks());

        // ── Add physical copies ─────────────────────────────────────────────
        BookItem ddiaCopy1 = bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        BookItem cleanCopy1 = bookService.addBookItem(clean.getIsbn(), new BookItem(clean));

        System.out.println("\nCopies of DDIA: " + bookService.getAllCopies(ddia.getIsbn()));

        // ── Checkout / return flow ──────────────────────────────────────────
        System.out.println("\nDDIA has available copy? " + bookService.hasAvailableCopy(ddia.getIsbn()));
        ddiaCopy1.checkout();
        System.out.println("After checking out one copy: " + ddiaCopy1);
        System.out.println("DDIA still has available copy? " + bookService.hasAvailableCopy(ddia.getIsbn()));
        ddiaCopy1.markReturned();
        System.out.println("After return: " + ddiaCopy1);

        // ── Search: by genre ────────────────────────────────────────────────
        List<Book> tech = bookService.search(SearchCriteria.builder()
            .genre("Technology")
            .build());
        System.out.println("\nTechnology books: " + tech);

        // ── Search: by title (partial) ──────────────────────────────────────
        List<Book> byTitle = bookService.search(SearchCriteria.builder()
            .title("clean")
            .build());
        System.out.println("Title contains 'clean': " + byTitle);

        // ── Search: available copies only ───────────────────────────────────
        cleanCopy1.checkout();
        List<Book> available = bookService.search(SearchCriteria.builder()
            .genre("Technology")
            .availableOnly(true)
            .build());
        System.out.println("Available Technology books (Clean Code checked out): " + available);
        cleanCopy1.markReturned();

        // ── Delete a book (cascades to copies + search index) ───────────────
        bookService.deleteBook(hobbit.getIsbn());
        System.out.println("\nAfter deleting The Hobbit: " + bookService.getAllBooks());

        // ═══════════════════════════════════════════════════════════════════
        // Step 2 — MemberService
        // ═══════════════════════════════════════════════════════════════════

        MemberRepository memberRepository = new InMemoryMemberRepository();
        MemberService memberService       = new MemberService(memberRepository);

        // ── Register members ────────────────────────────────────────────────
        Member alice = memberService.register(new Member("Alice", "alice@lib.com", "9001"));
        Member bob   = memberService.register(new Member("Bob",   "bob@lib.com",   "9002"));
        Member carol = memberService.register(new Member("Carol", "carol@lib.com", "9003"));

        System.out.println("\nAll members: " + memberService.getAllMembers());

        // ── Duplicate email rejected ─────────────────────────────────────────
        try {
            memberService.register(new Member("Alice2", "alice@lib.com", "9999"));
        } catch (DuplicateMemberException e) {
            System.out.println("\nDuplicate email blocked: " + e.getMessage());
        }

        // ── canBorrow reflects status ───────────────────────────────────────
        System.out.println("\nAlice canBorrow (ACTIVE)? " + alice.canBorrow());
        memberService.suspend("alice@lib.com");
        System.out.println("Alice canBorrow (SUSPENDED)? " + alice.canBorrow());
        memberService.reactivate("alice@lib.com");
        System.out.println("Alice canBorrow (reactivated)? " + alice.canBorrow());

        // ── Blacklist is terminal ───────────────────────────────────────────
        memberService.blacklist("bob@lib.com");
        System.out.println("\nBob status after blacklist: " + bob.getStatus());
        try {
            memberService.reactivate("bob@lib.com");
        } catch (IllegalStateException e) {
            System.out.println("Reactivate blacklisted member blocked: " + e.getMessage());
        }

        // ── Tier upgrade ────────────────────────────────────────────────────
        memberService.upgradeTier("carol@lib.com", MemberTier.LIBRARIAN);
        System.out.println("\nCarol tier after upgrade: " + carol.getTier());
        System.out.println("Carol isLibrarian? " + carol.isLibrarian());

        // ── Deregister ──────────────────────────────────────────────────────
        memberService.deregister("bob@lib.com");
        System.out.println("\nMembers after deregistering Bob: " + memberService.getAllMembers());
        // ═══════════════════════════════════════════════════════════════════
        // Step 3 — BorrowService
        // ═══════════════════════════════════════════════════════════════════

        BookLendingRepository lendingRepository   = new InMemoryBookLendingRepository();
        NotificationDispatcher notificationDispatcher = new NotificationDispatcher();
        BorrowService borrowService = new BorrowService(bookItemRepository, lendingRepository, notificationDispatcher);

        // re-register alice (deregistered above in Step 2 demo? no — only Bob was deregistered)
        Member borrower = memberService.getMember("alice@lib.com");

        // ── Happy path: borrow a book ────────────────────────────────────────
        System.out.println("\n--- BorrowService ---");
        BookLending lending1 = borrowService.borrowBook(borrower, ddia.getIsbn());
        System.out.println("Lending created: " + lending1);
        System.out.println("DDIA copy is now: " + lending1.getBookItem().getStatus());

        // ── Same member borrows a second book ───────────────────────────────
        BookLending lending2 = borrowService.borrowBook(borrower, clean.getIsbn());
        System.out.println("Second lending: " + lending2);

        // ── Active borrows for alice ─────────────────────────────────────────
        System.out.println("Active borrows for alice: "
                + lendingRepository.findActiveByMember("alice@lib.com").size());

        // ── Suspended member cannot borrow ──────────────────────────────────
        memberService.suspend("carol@lib.com");
        try {
            borrowService.borrowBook(memberService.getMember("carol@lib.com"), ddia.getIsbn());
        } catch (Exception e) {
            System.out.println("Suspended member blocked: " + e.getMessage());
        }
        memberService.reactivate("carol@lib.com");

        // ── Borrow limit: register 3 more members and push alice to limit ───
        // Alice already has 2 borrows; borrow 3 more to hit limit of 5
        for (int i = 3; i <= 5; i++) {
            // need extra copies of DDIA
            bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
        }
        borrowService.borrowBook(borrower, ddia.getIsbn());
        borrowService.borrowBook(borrower, ddia.getIsbn());
        borrowService.borrowBook(borrower, ddia.getIsbn());
        System.out.println("Alice active borrows (should be 5): "
                + lendingRepository.findActiveByMember("alice@lib.com").size());
        try {
            bookService.addBookItem(ddia.getIsbn(), new BookItem(ddia));
            borrowService.borrowBook(borrower, ddia.getIsbn());
        } catch (Exception e) {
            System.out.println("Borrow limit enforced: " + e.getMessage());
        }

    }
}

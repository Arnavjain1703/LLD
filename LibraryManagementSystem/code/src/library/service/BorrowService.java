package library.service;

import library.exception.BookNotAvailableException;
import library.exception.BorrowLimitExceededException;
import library.exception.MemberNotActiveException;
import library.model.BookItem;
import library.model.BookLending;
import library.model.Member;
import library.notification.NotificationDispatcher;
import library.repository.BookItemRepository;
import library.repository.BookLendingRepository;

/**
 * Handles the full borrow flow:
 *   1. Validate member — ACTIVE status + under borrow limit
 *      (outstanding-fine check is delegated to FineService and enforced before calling here)
 *   2. Find an available physical copy of the requested ISBN
 *   3. Checkout the copy (AVAILABLE → BORROWED)
 *   4. Persist the lending record
 *   5. Notify the member
 *
 * The whole method is synchronized to close the TOCTOU race between
 * findAvailableCopy and checkout — two concurrent requests must not
 * check out the same copy.
 */
public class BorrowService {

    private final BookItemRepository    bookItemRepository;
    private final BookLendingRepository lendingRepository;
    private final NotificationDispatcher notificationDispatcher;

    public BorrowService(BookItemRepository bookItemRepository,
                         BookLendingRepository lendingRepository,
                         NotificationDispatcher notificationDispatcher) {
        this.bookItemRepository    = bookItemRepository;
        this.lendingRepository     = lendingRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    public synchronized BookLending borrowBook(Member member, String isbn) {
        validateMember(member);

        BookItem copy = selectAvailableCopy(isbn);
        copy.checkout();   // AVAILABLE → BORROWED (domain method, synchronized on copy)

        BookLending lending = new BookLending(copy, member);
        lendingRepository.save(lending);

        notificationDispatcher.dispatch(member,
                "You borrowed \"" + copy.getBook().getTitle()
                + "\". Due: " + lending.getDueDate());

        return lending;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void validateMember(Member member) {
        if (!member.canBorrow()) {
            throw new MemberNotActiveException(member.getEmail());
        }
        int active = lendingRepository.findActiveByMember(member.getEmail()).size();
        if (active >= Member.MAX_BORROW_LIMIT) {
            throw new BorrowLimitExceededException(member.getEmail(), Member.MAX_BORROW_LIMIT);
        }
    }

    private BookItem selectAvailableCopy(String isbn) {
        return bookItemRepository.findAvailableCopy(isbn)
                .orElseThrow(() -> new BookNotAvailableException(isbn));
    }

}

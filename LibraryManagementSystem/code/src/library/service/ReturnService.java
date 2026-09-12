package library.service;

import library.exception.LendingNotFoundException;
import library.model.BookItem;
import library.model.BookItemStatus;
import library.model.BookLending;
import library.model.Fine;
import library.model.Member;
import library.notification.NotificationDispatcher;
import library.repository.BookLendingRepository;

import java.time.LocalDate;

/**
 * Handles the full return flow:
 *   1. Find the active lending by barcode
 *   2. Set returnDate — this is the date the fine is based on
 *   3. Transition copy: BORROWED → AVAILABLE (or RESERVED if queue non-empty)
 *   4. Notify next reservation holder if copy becomes RESERVED
 *   5. Calculate and pay fine via FineService (null if returned on time)
 *   6. Notify returning member
 *
 * Synchronized to prevent two concurrent returns of the same copy.
 */
public class ReturnService {

    private final BookLendingRepository  lendingRepository;
    private final FineService            fineService;
    private final ReservationService     reservationService;
    private final NotificationDispatcher notificationDispatcher;

    public ReturnService(BookLendingRepository lendingRepository,
                         FineService fineService,
                         ReservationService reservationService,
                         NotificationDispatcher notificationDispatcher) {
        this.lendingRepository     = lendingRepository;
        this.fineService           = fineService;
        this.reservationService    = reservationService;
        this.notificationDispatcher = notificationDispatcher;
    }

    public synchronized Fine returnBook(Member member, String barcode) {
        // 1. find the active lending for this copy
        BookLending lending = lendingRepository.findActiveByBarcode(barcode)
                .orElseThrow(() -> new LendingNotFoundException(
                        "No active lending for barcode: " + barcode));

        // 2. stamp return date — fine calculation is based on this
        lending.setReturnDate(LocalDate.now());
        lendingRepository.save(lending);

        // 3. transition the physical copy
        BookItem copy = lending.getBookItem();
        String   isbn = copy.getBook().getIsbn();

        copy.markReturned();   // BORROWED → AVAILABLE

        if (reservationService.hasWaiting(isbn)) {
            copy.markReserved();                       // AVAILABLE → RESERVED
            reservationService.notifyNext(isbn);       // notify head of queue
        }

        // 4. calculate fine and persist it as paid (null if returned on time)
        Fine fine = fineService.payFineForLending(lending);

        // 5. notify returning member
        notificationDispatcher.dispatch(member, buildMessage(lending, fine));

        return fine;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String buildMessage(BookLending lending, Fine fine) {
        String title = lending.getBookItem().getBook().getTitle();
        if (fine == null) {
            return "Thank you for returning \"" + title + "\". No fine — returned on time.";
        }
        return "Thank you for returning \"" + title + "\". "
                + "Overdue fine of $" + String.format("%.2f", fine.getAmount()) + " has been recorded.";
    }
}

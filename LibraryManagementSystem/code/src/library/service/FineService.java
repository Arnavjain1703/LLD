package library.service;

import library.exception.LendingNotFoundException;
import library.fine.FineStrategyFactory;
import library.model.BookLending;
import library.model.Fine;
import library.repository.FineRepository;

import java.util.List;

/**
 * FineService — single authority for fine calculation and payment.
 *
 * Unpaid fines are NEVER stored — they are calculated on-the-fly
 * from lending records whenever needed.
 *
 * FineRepository is a payment ledger — it only holds PAID fines.
 */
public class FineService {

    private final FineRepository      fineRepository;
    private final FineStrategyFactory fineStrategyFactory;

    public FineService(FineRepository fineRepository,
                       FineStrategyFactory fineStrategyFactory) {
        this.fineRepository      = fineRepository;
        this.fineStrategyFactory = fineStrategyFactory;
    }

    // ── On-the-fly calculation (no storage) ───────────────────────────────────

    /**
     * Projected fine for a single ACTIVE (not yet returned) lending as of today.
     * Returns 0.0 if already returned — use payFineForLending() for returned books.
     */
    public double getProjectedFine(BookLending lending) {
        if (lending.isReturned()) return 0.0;
        return computeAmount(lending);
    }

    /**
     * Total projected fine across all active lendings right now.
     * Caller fetches active lendings from BookLendingRepository and passes them in.
     */
    public double getTotalActiveFine(List<BookLending> activeLendings) {
        return activeLendings.stream()
                .mapToDouble(this::getProjectedFine)
                .sum();
    }

    /** Internal: calculates fine amount using returnDate if returned, today otherwise. */
    private double computeAmount(BookLending lending) {
        return fineStrategyFactory
                .getStrategy(lending.getMember().getTier())
                .calculate(lending);
    }

    // ── Payment (persists to repo) ────────────────────────────────────────────

    /**
     * Member pays the fine for a specific lending.
     * Calculates amount, creates a Fine record, marks it paid, and persists it.
     * Returns null if there is no fine due (lending not overdue).
     */
    public Fine payFineForLending(BookLending lending) {
        if (!lending.isReturned()) {
            throw new IllegalStateException(
                    "Cannot pay fine — book not yet returned: " + lending.getLendingId());
        }
        double amount = computeAmount(lending);
        if (amount <= 0) return null;   // no fine due

        Fine fine = new Fine(lending, lending.getMember(), amount);
        fine.pay();                     // mark paid immediately
        return fineRepository.save(fine);
    }

    /**
     * Librarian waives the fine for a specific lending.
     * Calculates amount, creates a Fine record, marks it waived, and persists it.
     * Returns null if there is no fine due.
     */
    public Fine waiveFineForLending(BookLending lending, String performedByEmail) {
        if (!lending.isReturned()) {
            throw new IllegalStateException(
                    "Cannot waive fine — book not yet returned: " + lending.getLendingId());
        }
        double amount = computeAmount(lending);
        if (amount <= 0) return null;

        Fine fine = new Fine(lending, lending.getMember(), amount);
        fine.waive();                   // mark waived immediately
        return fineRepository.save(fine);
    }

    // ── History ───────────────────────────────────────────────────────────────

    /** Full payment/waiver history for a member. */
    public List<Fine> getFineHistory(String memberEmail) {
        return fineRepository.findByMember(memberEmail);
    }
}

package library.repository;

import library.model.BookLending;
import java.util.List;
import java.util.Optional;

public interface BookLendingRepository {
    BookLending save(BookLending lending);
    Optional<BookLending> findById(String lendingId);
    /** All active (not yet returned) lendings for a member. */
    List<BookLending> findActiveByMember(String memberEmail);
    /** The single active lending for a specific copy barcode. */
    Optional<BookLending> findActiveByBarcode(String barcode);
    /** Full history — active + returned — for a member. */
    List<BookLending> findAllByMember(String memberEmail);
}

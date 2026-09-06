package library.repository;

import library.model.BookItem;

import java.util.List;
import java.util.Optional;

public interface BookItemRepository {
    BookItem save(String isbn, BookItem bookItem);
    Optional<BookItem> findByBarcode(String barcode);
    void delete(String barcode);
    void deleteAllCopies(String isbn);           // used when a Book is deleted

    Optional<BookItem> findAvailableCopy(String isbn);
    boolean hasAvailableCopy(String isbn);
    List<BookItem> findAllCopies(String isbn);
}

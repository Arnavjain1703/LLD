package library.repository;

import library.model.Book;
import library.model.BookItem;

import java.util.List;
import java.util.Optional;

public interface BookRepository {
    Book save(Book book);
    Optional<Book> findByIsbn(String isbn);
    List<Book> findAll();
    void delete(String isbn);

    BookItem saveBookItem(String isbn, BookItem bookItem);
    Optional<BookItem> findBookItemByBarcode(String barcode);
    void deleteBookItem(String barcode);
}

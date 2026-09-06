package library.repository;

import library.exception.BookNotFoundException;
import library.model.Book;
import library.model.BookItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookRepository implements BookRepository {

    // ConcurrentHashMap — thread-safe O(1) avg for get/put/remove
    // segments are locked independently — better throughput than synchronized HashMap
    private final Map<String, Book>     bookStore     = new ConcurrentHashMap<>();
    private final Map<String, BookItem> bookItemStore = new ConcurrentHashMap<>();

    @Override
    public Book save(Book book) {
        bookStore.put(book.getIsbn(), book);
        return book;
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.ofNullable(bookStore.get(isbn));
    }

    @Override
    public List<Book> findAll() {
        // snapshot — safe for caller to iterate without holding a lock
        return Collections.unmodifiableList(new ArrayList<>(bookStore.values()));
    }

    @Override
    public synchronized void delete(String isbn) {
        // synchronized — compound op: remove book + remove all its copies
        // must be atomic — another thread must not see a half-deleted state
        Book book = bookStore.remove(isbn);
        if (book == null) {
            throw new BookNotFoundException("Book not found with ISBN: " + isbn);
        }
        book.getCopies().forEach(item -> bookItemStore.remove(item.getBarcode()));
    }

    @Override
    public synchronized BookItem saveBookItem(String isbn, BookItem bookItem) {
        // synchronized — compound op: validate isbn + write to both book.copies + bookItemStore
        Book book = bookStore.get(isbn);
        if (book == null) {
            throw new BookNotFoundException("Cannot add copy — Book not found with ISBN: " + isbn);
        }
        book.addCopy(bookItem);
        bookItemStore.put(bookItem.getBarcode(), bookItem);
        return bookItem;
    }

    @Override
    public Optional<BookItem> findBookItemByBarcode(String barcode) {
        return Optional.ofNullable(bookItemStore.get(barcode));
    }

    @Override
    public synchronized void deleteBookItem(String barcode) {
        // synchronized — compound op: remove from itemStore + remove from Book.copies
        BookItem item = bookItemStore.remove(barcode);
        if (item == null) {
            throw new BookNotFoundException("BookItem not found with barcode: " + barcode);
        }
        item.getBook().removeCopy(barcode);
    }
}

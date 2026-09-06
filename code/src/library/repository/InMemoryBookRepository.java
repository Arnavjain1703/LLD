package library.repository;

import library.exception.BookNotFoundException;
import library.model.Book;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookRepository implements BookRepository {

    // isbn → Book — O(1) lookup by ISBN
    private final Map<String, Book> bookStore = new ConcurrentHashMap<>();

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
    public void delete(String isbn) {
        if (bookStore.remove(isbn) == null) {
            throw new BookNotFoundException("Book not found with ISBN: " + isbn);
        }
    }
}

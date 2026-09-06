package library.service;

import library.exception.BookNotFoundException;
import library.model.Book;
import library.model.BookItem;
import library.repository.BookItemRepository;
import library.repository.BookRepository;
import library.search.CatalogSearchService;
import library.search.SearchCriteria;

import java.util.List;

public class BookService {

    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final CatalogSearchService catalogSearchService;

    public BookService(BookRepository bookRepository,
                       BookItemRepository bookItemRepository,
                       CatalogSearchService catalogSearchService) {
        this.bookRepository       = bookRepository;
        this.bookItemRepository   = bookItemRepository;
        this.catalogSearchService = catalogSearchService;
    }

    // ── Book operations ───────────────────────────────────────────────────────

    // synchronized — save to repo + index to catalog must appear atomic
    public synchronized Book addBook(Book book) {
        Book saved = bookRepository.save(book);
        catalogSearchService.indexBook(saved);
        return saved;
    }

    // synchronized — 3 operations must appear atomic:
    // 1. delete all copies  2. delete book  3. remove from search index
    public synchronized void deleteBook(String isbn) {
        bookItemRepository.deleteAllCopies(isbn); // copies first — no orphaned items
        bookRepository.delete(isbn);
        catalogSearchService.removeBook(isbn);
    }

    public Book getBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
               .orElseThrow(() -> new BookNotFoundException(
                   "Book not found with ISBN: " + isbn
               ));
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> search(SearchCriteria criteria) {
        return catalogSearchService.search(criteria);
    }

    // ── BookItem operations ───────────────────────────────────────────────────

    public BookItem addBookItem(String isbn, BookItem bookItem) {
        // validate book exists before adding a copy
        bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new BookNotFoundException(
                "Cannot add copy — Book not found with ISBN: " + isbn
            ));
        return bookItemRepository.save(isbn, bookItem);
    }

    public void deleteBookItem(String barcode) {
        bookItemRepository.delete(barcode);
    }

    public List<BookItem> getAllCopies(String isbn) {
        return bookItemRepository.findAllCopies(isbn);
    }

    public boolean hasAvailableCopy(String isbn) {
        return bookItemRepository.hasAvailableCopy(isbn);
    }
}

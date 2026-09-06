package library.service;

import library.exception.BookNotFoundException;
import library.model.Book;
import library.model.BookItem;
import library.repository.BookRepository;
import library.search.CatalogSearchService;
import library.search.SearchCriteria;

import java.util.List;

public class BookService {

    private final BookRepository bookRepository;
    private final CatalogSearchService catalogSearchService;

    public BookService(BookRepository bookRepository,
                       CatalogSearchService catalogSearchService) {
        this.bookRepository       = bookRepository;
        this.catalogSearchService = catalogSearchService;
    }

    // synchronized — save to repo + index to catalog must appear atomic
    // a search between the two calls would miss the newly added book
    public synchronized Book addBook(Book book) {
        Book saved = bookRepository.save(book);
        catalogSearchService.indexBook(saved);
        return saved;
    }

    public BookItem addBookItem(String isbn, BookItem bookItem) {
        // repo validates isbn exists — throws BookNotFoundException if missing
        return bookRepository.saveBookItem(isbn, bookItem);
    }

    // synchronized — delete from repo + remove from index must appear atomic
    // a search between the two calls would find a book that no longer exists in repo
    public synchronized void deleteBook(String isbn) {
        bookRepository.delete(isbn);
        catalogSearchService.removeBook(isbn);
    }

    public void deleteBookItem(String barcode) {
        bookRepository.deleteBookItem(barcode);
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
}

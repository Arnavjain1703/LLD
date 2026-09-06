package library.search;

import library.model.Book;
import library.repository.BookItemRepository;
import library.search.handler.SearchHandler;
import library.search.handler.IsbnSearchHandler;
import library.search.handler.TitleSearchHandler;
import library.search.handler.AuthorSearchHandler;
import library.search.handler.GenreSearchHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CatalogSearchService {

    // byISBN is the master index — single source of truth for all books
    // used for O(1) direct ISBN lookup and as the base collection for filtered search
    private final Map<String, Book> byISBN = new ConcurrentHashMap<>();

    // handlers — each knows how to check if a Book matches one criteria field
    // adding a new search field = new handler class only, nothing else changes
    private final List<SearchHandler> handlers;

    // injected to check physical availability without coupling Book to copies
    private final BookItemRepository bookItemRepository;

    public CatalogSearchService(BookItemRepository bookItemRepository) {
        this.bookItemRepository = bookItemRepository;
        handlers = List.of(
            new IsbnSearchHandler(),
            new TitleSearchHandler(),
            new AuthorSearchHandler(),
            new GenreSearchHandler()
        );
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<Book> search(SearchCriteria criteria) {

        // collect only handlers relevant to the provided criteria
        List<SearchHandler> matched = handlers.stream()
            .filter(h -> h.canHandle(criteria))
            .collect(Collectors.toList());

        // no criteria provided — return all books
        if (matched.isEmpty()) {
            return paginate(
                new ArrayList<>(byISBN.values()),
                criteria.getPage(), criteria.getPageSize()
            );
        }

        // single filter pass — book must satisfy ALL active criteria (intersection)
        List<Book> results = byISBN.values().stream()
            .filter(book -> matched.stream().allMatch(h -> h.matches(book, criteria)))
            .collect(Collectors.toList());

        // availability post-filter — delegates to BookItemRepository, not Book
        if (criteria.isAvailableOnly()) {
            results = results.stream()
                             .filter(book -> bookItemRepository.hasAvailableCopy(book.getIsbn()))
                             .collect(Collectors.toList());
        }

        return paginate(results, criteria.getPage(), criteria.getPageSize());
    }

    // ── Index management ──────────────────────────────────────────────────────

    // synchronized — write to index must be atomic
    public synchronized void indexBook(Book book) {
        byISBN.put(book.getIsbn(), book);
    }

    // synchronized — remove from index must be atomic
    public synchronized void removeBook(String isbn) {
        byISBN.remove(isbn);
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    private List<Book> paginate(List<Book> results, int page, int pageSize) {
        if (pageSize <= 0) return results;
        int from = Math.min(page * pageSize, results.size());
        int to   = Math.min(from + pageSize, results.size());
        return results.subList(from, to);
    }
}
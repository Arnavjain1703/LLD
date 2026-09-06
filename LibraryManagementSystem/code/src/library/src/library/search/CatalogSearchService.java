package library.search;

import library.model.Book;
import library.search.handler.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CatalogSearchService {

    private final Map<String, List<Book>> byTitle  = new ConcurrentHashMap<>();
    private final Map<String, List<Book>> byAuthor = new ConcurrentHashMap<>();
    private final Map<String, Book>       byISBN   = new ConcurrentHashMap<>();
    private final Map<String, List<Book>> byGenre  = new ConcurrentHashMap<>();

    // ordered list of handlers — each handles one search field
    // adding a new field = new handler class + register here, nothing else changes
    private final List<SearchHandler> handlers;

    public CatalogSearchService() {
        handlers = List.of(
            new IsbnSearchHandler(byISBN),
            new TitleSearchHandler(byTitle),
            new AuthorSearchHandler(byAuthor),
            new GenreSearchHandler(byGenre)
        );
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<Book> search(SearchCriteria criteria) {

        // find all handlers that can handle the given criteria fields
        List<SearchHandler> matched = handlers.stream()
            .filter(h -> h.canHandle(criteria))
            .collect(Collectors.toList());

        // no criteria provided — return all books (full scan)
        if (matched.isEmpty()) {
            return paginate(
                new ArrayList<>(byISBN.values()),
                criteria.getPage(), criteria.getPageSize()
            );
        }

        // run all matched handlers independently
        List<List<Book>> allResults = matched.stream()
            .map(h -> h.handle(criteria))
            .collect(Collectors.toList());

        // intersect — book must satisfy ALL provided criteria
        Set<Book> intersection = new HashSet<>(allResults.get(0));
        for (int i = 1; i < allResults.size(); i++) {
            intersection.retainAll(new HashSet<>(allResults.get(i)));
        }

        List<Book> results = new ArrayList<>(intersection);

        // availability post-filter — applied after intersection
        if (criteria.isAvailableOnly()) {
            results = results.stream()
                             .filter(Book::hasAvailableCopy)
                             .collect(Collectors.toList());
        }

        return paginate(results, criteria.getPage(), criteria.getPageSize());
    }

    // ── Index management ──────────────────────────────────────────────────────

    // synchronized — compound write across 4 maps must be atomic
    public synchronized void indexBook(Book book) {
        byISBN.put(book.getIsbn(), book);

        byTitle.computeIfAbsent(
            book.getTitle().toLowerCase(), k -> new ArrayList<>()
        ).add(book);

        byGenre.computeIfAbsent(
            book.getGenre().toLowerCase(), k -> new ArrayList<>()
        ).add(book);

        book.getAuthors().forEach(author ->
            byAuthor.computeIfAbsent(
                author.toLowerCase(), k -> new ArrayList<>()
            ).add(book)
        );
    }

    // synchronized — compound delete across 4 maps must be atomic
    public synchronized void removeBook(String isbn) {
        Book book = byISBN.remove(isbn);
        if (book == null) return;

        byTitle.getOrDefault(
            book.getTitle().toLowerCase(), Collections.emptyList()
        ).remove(book);

        byGenre.getOrDefault(
            book.getGenre().toLowerCase(), Collections.emptyList()
        ).remove(book);

        book.getAuthors().forEach(author ->
            byAuthor.getOrDefault(
                author.toLowerCase(), Collections.emptyList()
            ).remove(book)
        );
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    private List<Book> paginate(List<Book> results, int page, int pageSize) {
        if (pageSize <= 0) return results;
        int from = Math.min(page * pageSize, results.size());
        int to   = Math.min(from + pageSize, results.size());
        return results.subList(from, to);
    }
}

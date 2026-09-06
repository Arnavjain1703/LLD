package library.search;

import library.model.Book;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CatalogSearchService {

    // ConcurrentHashMap — thread-safe reads without locking
    // individual get() calls are safe across threads
    // compound writes (indexBook/removeBook) are synchronized separately
    private final Map<String, List<Book>> byTitle  = new ConcurrentHashMap<>();
    private final Map<String, List<Book>> byAuthor = new ConcurrentHashMap<>();
    private final Map<String, Book>       byISBN   = new ConcurrentHashMap<>();
    private final Map<String, List<Book>> byGenre  = new ConcurrentHashMap<>();

    // synchronized — compound write across 4 maps must be atomic
    // partial index = stale or inconsistent search results
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

    // read-only — no lock needed
    // ConcurrentHashMap.get() is safe without synchronization
    public List<Book> search(SearchCriteria criteria) {
        List<Book> results;

        if (criteria.getIsbn() != null) {
            // O(1) — most specific, check ISBN first
            Book book = byISBN.get(criteria.getIsbn());
            results = book != null ? List.of(book) : Collections.emptyList();

        } else if (criteria.getTitle() != null) {
            // O(1) map lookup + O(n) copy for safe iteration
            results = new ArrayList<>(
                byTitle.getOrDefault(criteria.getTitle().toLowerCase(), Collections.emptyList())
            );

        } else if (criteria.getAuthor() != null) {
            results = new ArrayList<>(
                byAuthor.getOrDefault(criteria.getAuthor().toLowerCase(), Collections.emptyList())
            );

        } else if (criteria.getGenre() != null) {
            results = new ArrayList<>(
                byGenre.getOrDefault(criteria.getGenre().toLowerCase(), Collections.emptyList())
            );

        } else {
            // no filter — full scan O(n)
            results = new ArrayList<>(byISBN.values());
        }

        // secondary filter — availability
        if (criteria.isAvailableOnly()) {
            results = results.stream()
                             .filter(Book::hasAvailableCopy)
                             .collect(Collectors.toList());
        }

        return paginate(results, criteria.getPage(), criteria.getPageSize());
    }

    private List<Book> paginate(List<Book> results, int page, int pageSize) {
        if (pageSize <= 0) return results; // no pagination requested
        int from = Math.min(page * pageSize, results.size());
        int to   = Math.min(from + pageSize, results.size());
        return results.subList(from, to);
    }
}

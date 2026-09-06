package library.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class Book {

    private final String isbn;
    private final String title;
    private final List<String> authors;
    private final String genre;
    private final String publisher;
    private final String language;
    private final String edition;

    // CopyOnWriteArrayList — thread-safe for read-heavy workload
    // reads (stream, getAvailableCopy) need no lock
    // writes (addCopy, removeCopy) are infrequent — copy-on-write cost is acceptable
    private final List<BookItem> copies = new CopyOnWriteArrayList<>();

    public Book(String isbn, String title, List<String> authors,
                String genre, String publisher, String language, String edition) {
        this.isbn      = isbn;
        this.title     = title;
        this.authors   = List.copyOf(authors); // immutable — authors never change
        this.genre     = genre;
        this.publisher = publisher;
        this.language  = language;
        this.edition   = edition;
    }

    public void addCopy(BookItem item) {
        copies.add(item); // CopyOnWriteArrayList handles thread safety
    }

    public void removeCopy(String barcode) {
        copies.removeIf(item -> item.getBarcode().equals(barcode));
    }

    // safe without lock — CopyOnWriteArrayList uses snapshot iteration
    public Optional<BookItem> getAvailableCopy() {
        return copies.stream()
                     .filter(BookItem::isAvailable)
                     .findFirst();
    }

    public boolean hasAvailableCopy() {
        return copies.stream().anyMatch(BookItem::isAvailable);
    }

    public String getIsbn()            { return isbn; }
    public String getTitle()           { return title; }
    public List<String> getAuthors()   { return authors; }
    public String getGenre()           { return genre; }
    public String getPublisher()       { return publisher; }
    public String getLanguage()        { return language; }
    public String getEdition()         { return edition; }
    public List<BookItem> getCopies()  { return Collections.unmodifiableList(copies); }

    @Override
    public String toString() {
        return "Book{isbn='" + isbn + "', title='" + title + "'}";
    }
}

package library.model;

import java.util.List;

public class Book {

    private final String isbn;
    private final String title;
    private final List<String> authors;
    private final String genre;
    private final String publisher;
    private final String language;
    private final String edition;

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

    // pure value object — no copies list, no mutation methods
    // copy management is entirely owned by BookRepository

    public String getIsbn()          { return isbn; }
    public String getTitle()         { return title; }
    public List<String> getAuthors() { return authors; }
    public String getGenre()         { return genre; }
    public String getPublisher()     { return publisher; }
    public String getLanguage()      { return language; }
    public String getEdition()       { return edition; }

    @Override
    public String toString() {
        return "Book{isbn='" + isbn + "', title='" + title + "'}";
    }
}

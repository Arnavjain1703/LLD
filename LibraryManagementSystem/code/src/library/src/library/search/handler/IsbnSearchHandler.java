package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IsbnSearchHandler implements SearchHandler {

    private final Map<String, Book> byISBN;

    public IsbnSearchHandler(Map<String, Book> byISBN) {
        this.byISBN = byISBN;
    }

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getIsbn() != null;
    }

    @Override
    public List<Book> handle(SearchCriteria criteria) {
        Book book = byISBN.get(criteria.getIsbn());
        return book != null ? List.of(book) : Collections.emptyList();
    }
}

package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AuthorSearchHandler implements SearchHandler {

    private final Map<String, List<Book>> byAuthor;

    public AuthorSearchHandler(Map<String, List<Book>> byAuthor) {
        this.byAuthor = byAuthor;
    }

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getAuthor() != null;
    }

    @Override
    public List<Book> handle(SearchCriteria criteria) {
        return new ArrayList<>(
            byAuthor.getOrDefault(criteria.getAuthor().toLowerCase(), Collections.emptyList())
        );
    }
}

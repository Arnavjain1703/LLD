package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GenreSearchHandler implements SearchHandler {

    private final Map<String, List<Book>> byGenre;

    public GenreSearchHandler(Map<String, List<Book>> byGenre) {
        this.byGenre = byGenre;
    }

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getGenre() != null;
    }

    @Override
    public List<Book> handle(SearchCriteria criteria) {
        return new ArrayList<>(
            byGenre.getOrDefault(criteria.getGenre().toLowerCase(), Collections.emptyList())
        );
    }
}

package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TitleSearchHandler implements SearchHandler {

    private final Map<String, List<Book>> byTitle;

    public TitleSearchHandler(Map<String, List<Book>> byTitle) {
        this.byTitle = byTitle;
    }

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getTitle() != null;
    }

    @Override
    public List<Book> handle(SearchCriteria criteria) {
        return new ArrayList<>(
            byTitle.getOrDefault(criteria.getTitle().toLowerCase(), Collections.emptyList())
        );
    }
}

package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

public class TitleSearchHandler implements SearchHandler {

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getTitle() != null;
    }

    @Override
    public boolean matches(Book book, SearchCriteria criteria) {
        // contains — partial match e.g. "Clean" matches "Clean Code"
        return book.getTitle().toLowerCase()
                   .contains(criteria.getTitle().toLowerCase());
    }
}

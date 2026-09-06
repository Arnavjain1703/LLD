package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

public class IsbnSearchHandler implements SearchHandler {

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getIsbn() != null;
    }

    @Override
    public boolean matches(Book book, SearchCriteria criteria) {
        return book.getIsbn().equals(criteria.getIsbn());
    }
}

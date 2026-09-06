package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

public class GenreSearchHandler implements SearchHandler {

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getGenre() != null;
    }

    @Override
    public boolean matches(Book book, SearchCriteria criteria) {
        return book.getGenre().toLowerCase()
                   .contains(criteria.getGenre().toLowerCase());
    }
}

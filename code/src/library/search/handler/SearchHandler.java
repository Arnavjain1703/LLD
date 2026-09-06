package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

public interface SearchHandler {
    boolean canHandle(SearchCriteria criteria);
    boolean matches(Book book, SearchCriteria criteria);
}

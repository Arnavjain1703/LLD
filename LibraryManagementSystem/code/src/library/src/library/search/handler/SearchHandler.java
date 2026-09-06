package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

import java.util.List;

public interface SearchHandler {
    boolean canHandle(SearchCriteria criteria);
    List<Book> handle(SearchCriteria criteria);
}

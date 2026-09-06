package library.search.handler;

import library.model.Book;
import library.search.SearchCriteria;

public class AuthorSearchHandler implements SearchHandler {

    @Override
    public boolean canHandle(SearchCriteria criteria) {
        return criteria.getAuthor() != null;
    }

    @Override
    public boolean matches(Book book, SearchCriteria criteria) {
        // any author on the book matches the search term
        return book.getAuthors().stream()
                   .anyMatch(a -> a.toLowerCase()
                   .contains(criteria.getAuthor().toLowerCase()));
    }
}

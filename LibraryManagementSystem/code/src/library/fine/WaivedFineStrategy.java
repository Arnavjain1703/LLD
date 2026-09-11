package library.fine;

import library.model.BookLending;

/** Librarians are never charged a fine. */
public class WaivedFineStrategy implements FineStrategy {

    @Override
    public double calculate(BookLending lending) {
        return 0.0;
    }
}

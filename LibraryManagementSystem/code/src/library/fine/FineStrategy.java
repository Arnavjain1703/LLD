package library.fine;

import library.model.BookLending;

public interface FineStrategy {
    double calculate(BookLending lending);
}

package library.exception;

public class BorrowLimitExceededException extends RuntimeException {
    public BorrowLimitExceededException(String email, int limit) {
        super("Member " + email + " has reached the borrow limit of " + limit);
    }
}

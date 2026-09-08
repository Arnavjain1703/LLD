package library.exception;

public class LendingNotFoundException extends RuntimeException {
    public LendingNotFoundException(String detail) {
        super("Lending not found: " + detail);
    }
}

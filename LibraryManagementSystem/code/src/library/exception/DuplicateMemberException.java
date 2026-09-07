package library.exception;

public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException(String email) {
        super("Member already registered with email: " + email);
    }
}

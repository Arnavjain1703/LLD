package library.exception;

public class MemberNotActiveException extends RuntimeException {
    public MemberNotActiveException(String email) {
        super("Member is not active: " + email);
    }
}

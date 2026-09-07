package library.exception;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String email) {
        super("Member not found: " + email);
    }
}

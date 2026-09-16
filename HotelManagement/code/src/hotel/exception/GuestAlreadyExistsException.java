package hotel.exception;

public class GuestAlreadyExistsException extends RuntimeException {
    public GuestAlreadyExistsException(String message) { super(message); }
}

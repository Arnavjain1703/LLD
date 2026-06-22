package vendingmachine.exception;

public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String action, String state) {
        super(String.format("Cannot perform [%s] in current state: %s", action, state));
    }
}

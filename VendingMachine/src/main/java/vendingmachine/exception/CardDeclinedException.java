package vendingmachine.exception;

public class CardDeclinedException extends RuntimeException {
    public CardDeclinedException(String reason) {
        super("Card declined: " + reason);
    }
}

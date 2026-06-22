package vendingmachine.payment;

/**
 * Abstraction over the actual card network (Visa/MC/bank).
 * Allows injection of real or simulated processors.
 */
public interface CardProcessor {
    /**
     * Authorize and charge the card for the given amount in cents.
     * @return true if approved
     */
    boolean charge(String cardToken, long amountInCents);

    /** Reverse a prior charge. */
    void refund(String cardToken, long amountInCents);

    /** Tokenize a raw card number. Returns an opaque token. */
    String tokenize(String cardNumber);
}
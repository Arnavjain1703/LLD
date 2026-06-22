package vendingmachine.payment;

import vendingmachine.exception.CardDeclinedException;
import vendingmachine.exception.InvalidStateException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Card payment strategy — tokenizes card, charges exact price.
 * No change is returned; any overpayment is not applicable (card charges exact amount).
 * Thread-safe via AtomicReference on the card token.
 */
public class CardPaymentStrategy implements PaymentStrategy {

    private final CardProcessor processor;
    // null = no card inserted; non-null = card ready to charge
    private final AtomicReference<String> cardToken = new AtomicReference<>(null);
    // Track the last charged amount for potential refund
    private volatile long lastChargedCents = 0;

    public CardPaymentStrategy(CardProcessor processor) {
        this.processor = processor;
    }

    /**
     * Swipe or tap a card. Tokenizes immediately but does NOT charge yet.
     */
    public void swipeCard(String cardNumber) {
        String token = processor.tokenize(cardNumber);
        cardToken.set(token);
        System.out.printf("[CARD] Card accepted — token: %s%n", token);
    }

    public boolean isCardInserted() {
        return cardToken.get() != null;
    }

    @Override
    public PaymentType getType() { return PaymentType.CARD; }

    @Override
    public boolean processPayment(long priceInCents) {
        String token = cardToken.get();
        if (token == null) {
            throw new InvalidStateException("processPayment", "CardPayment — no card inserted");
        }
        boolean approved = processor.charge(token, priceInCents);
        if (!approved) {
            throw new CardDeclinedException("authorization failed for token " + token);
        }
        lastChargedCents = priceInCents;
        return true;
    }

    @Override
    public long collectChangeInCents(long priceInCents) {
        // Card always charges exact amount — no change
        return 0;
    }

    @Override
    public long refund() {
        String token = cardToken.getAndSet(null);
        if (token != null && lastChargedCents > 0) {
            processor.refund(token, lastChargedCents);
            long refunded = lastChargedCents;
            lastChargedCents = 0;
            return refunded;
        }
        return 0;
    }
}
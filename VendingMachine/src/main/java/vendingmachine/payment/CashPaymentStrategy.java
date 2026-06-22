package vendingmachine.payment;

import vendingmachine.model.Coin;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Cash payment strategy — tracks inserted coins using AtomicLong (cents).
 * Thread-safe: coin insertion uses CAS so concurrent insertions never lose a coin.
 */
public class CashPaymentStrategy implements PaymentStrategy {

    // All amounts in cents to avoid floating-point arithmetic
    private final AtomicLong insertedCents = new AtomicLong(0);

    public void insertCoin(Coin coin) {
        long added = insertedCents.addAndGet(coin.getValueInCents());
        System.out.printf("[CASH] Inserted %s | Total: $%.2f%n", coin, added / 100.0);
    }

    public long getInsertedCents() {
        return insertedCents.get();
    }

    @Override
    public PaymentType getType() { return PaymentType.CASH; }

    @Override
    public boolean processPayment(long priceInCents) {
        return insertedCents.get() >= priceInCents;
    }

    @Override
    public long collectChangeInCents(long priceInCents) {
        // Atomically drain the inserted amount and return the change
        long totalInserted = insertedCents.getAndSet(0);
        long change = totalInserted - priceInCents;
        if (change > 0) {
            System.out.printf("[CASH] Returning change: $%.2f%n", change / 100.0);
        }
        return Math.max(change, 0);
    }

    @Override
    public long refund() {
        long refund = insertedCents.getAndSet(0);
        if (refund > 0) {
            System.out.printf("[CASH] Refunded: $%.2f%n", refund / 100.0);
        }
        return refund;
    }
}
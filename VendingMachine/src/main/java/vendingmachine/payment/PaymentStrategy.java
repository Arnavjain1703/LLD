package vendingmachine.payment;

/**
 * Strategy interface for payment methods.
 * Implementations handle how payment is collected and refunded.
 */
public interface PaymentStrategy {
    PaymentType getType();

    /**
     * Attempt to pay the given amount (in cents).
     * @return true if payment is complete and sufficient.
     */
    boolean processPayment(long amountInCents);

    /**
     * Refund any collected funds.
     * For cash: returns coin amount in cents. For card: reverses charge.
     * @return amount refunded in cents
     */
    long refund();

    /**
     * Compute change after a successful payment (only meaningful for cash).
     * @return change in cents
     */
    long collectChangeInCents(long priceInCents);
}
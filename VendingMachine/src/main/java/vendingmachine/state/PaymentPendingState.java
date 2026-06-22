package vendingmachine.state;

import vendingmachine.TransactionContext;
import vendingmachine.exception.CardDeclinedException;
import vendingmachine.exception.InvalidStateException;
import vendingmachine.model.Coin;
import vendingmachine.payment.CardPaymentStrategy;
import vendingmachine.payment.CashPaymentStrategy;
import vendingmachine.payment.PaymentStrategy;

/**
 * PAYMENT_PENDING — collecting payment.
 *
 * CASH path: accept coins one at a time.
 *   - total < price  → stay in this state, ask for more (never fail/reject the user)
 *   - total >= price → auto-advance to DispensingState, return change if any
 *
 * CARD path: swipe → charge → advance to DispensingState.
 *   - Declined       → stay in this state, user can try another card or cancel
 */
public class PaymentPendingState extends VendingMachineState {

    public PaymentPendingState(TransactionContext ctx) {
        super(ctx);
    }

    @Override
    public void insertCoin(Coin coin) {
        PaymentStrategy strategy = ctx.getPaymentStrategy();
        if (!(strategy instanceof CashPaymentStrategy cash)) {
            throw new InvalidStateException("insertCoin — card payment selected, not cash", getStateName());
        }

        cash.insertCoin(coin);
        long priceInCents = ctx.getSelectedRack().getProduct().getPriceInCents();

        if (cash.processPayment(priceInCents)) {
            // Sufficient funds — trigger dispense (change is handled inside execute())
            new DispensingState(ctx).execute();
        } else {
            // Ask for more — do NOT fail, stay in PaymentPending
            double remaining = (priceInCents - cash.getInsertedCents()) / 100.0;
            ctx.display(String.format("Still need $%.2f — please insert more coins.", remaining));
        }
    }

    @Override
    public void swipeCard(String cardNumber) {
        PaymentStrategy strategy = ctx.getPaymentStrategy();
        if (!(strategy instanceof CardPaymentStrategy card)) {
            throw new InvalidStateException("swipeCard — cash payment selected, not card", getStateName());
        }

        card.swipeCard(cardNumber);
        try {
            card.processPayment(ctx.getSelectedRack().getProduct().getPriceInCents());
            new DispensingState(ctx).execute();
        } catch (CardDeclinedException e) {
            // Stay in PaymentPending — user can try another card or cancel
            ctx.display(e.getMessage() + " — try another card or cancel.");
        }
    }

    @Override
    public void cancel() {
        long refunded = ctx.getPaymentStrategy().refund();
        ctx.reset();
        if (refunded > 0) {
            ctx.display(String.format("Cancelled. Refunded $%.2f.", refunded / 100.0));
        } else {
            ctx.display("Cancelled.");
        }
    }

    @Override
    public String getStateName() { return "PAYMENT_PENDING"; }
}

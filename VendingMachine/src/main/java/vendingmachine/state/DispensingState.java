package vendingmachine.state;

import vendingmachine.TransactionContext;
import vendingmachine.exception.OutOfStockException;
import vendingmachine.inventory.Rack;

/**
 * DISPENSING — machine is physically dispensing the product.
 * All user-facing actions are blocked in this state.
 *
 * Dispense logic lives here (not in VendingMachine) — the state that owns
 * dispensing should own the logic.
 */
public class DispensingState extends VendingMachineState {

    public DispensingState(TransactionContext ctx) {
        super(ctx);
    }

    /**
     * Sets this as the current state and runs the full dispense sequence.
     * Called by PaymentPendingState immediately after payment is confirmed.
     * Auto-resets to Idle on completion.
     *
     * Two concurrent threads can both reach execute() if they were both in
     * PaymentPendingState before the first one completed dispense + reset.
     * The null-check on rack handles this: the second thread sees selectedRack=null
     * (already cleared by reset()) and exits quietly — its cash is already spent
     * via AtomicLong but the product was not double-dispensed.
     *
     * Throws OutOfStockException if rack.deduct() fails — can happen in a flash-sale
     * race where multiple machines share one inventory.
     */
    void execute() {
        ctx.setState(this);

        Rack rack = ctx.getSelectedRack();
        if (rack == null) {
            // Another thread already dispensed and reset this context — nothing to do.
            return;
        }

        if (!rack.deduct()) {
            // Race: item was available at selectRack() but another machine got it first.
            ctx.getPaymentStrategy().refund();
            ctx.reset();
            throw new OutOfStockException(rack.getProduct());
        }

        long change = ctx.getPaymentStrategy().collectChangeInCents(rack.getProduct().getPriceInCents());
        ctx.display(String.format(">>> Dispensing: %s | Change: $%.2f <<<",
                rack.getProduct().getName(), change / 100.0));
        ctx.reset();
    }

    @Override
    public String getStateName() { return "DISPENSING"; }
}

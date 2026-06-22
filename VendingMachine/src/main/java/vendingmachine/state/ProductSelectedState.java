package vendingmachine.state;

import vendingmachine.TransactionContext;
import vendingmachine.payment.CardPaymentStrategy;
import vendingmachine.payment.CashPaymentStrategy;
import vendingmachine.payment.PaymentStrategy;

/** PRODUCT_SELECTED — rack chosen, waiting for payment method selection. */
public class ProductSelectedState extends VendingMachineState {

    public ProductSelectedState(TransactionContext ctx) {
        super(ctx);
    }

    @Override
    public void selectRack(String rackId) {
        // User changed mind — reset cleanly, re-enter Idle, then re-select
        ctx.reset();
        ctx.getState().selectRack(rackId);
    }

    @Override
    public void selectPaymentMethod(PaymentStrategy strategy) {
        ctx.setPaymentStrategy(strategy);
        ctx.setState(new PaymentPendingState(ctx));

        if (strategy instanceof CashPaymentStrategy) {
            ctx.display(String.format("Cash selected. Please insert $%.2f",
                    ctx.getSelectedRack().getProduct().getPrice()));
        } else if (strategy instanceof CardPaymentStrategy) {
            ctx.display("Card selected. Please swipe / tap your card.");
        }
    }

    @Override
    public void cancel() {
        ctx.reset();
        ctx.display("Cancelled. No payment was made.");
    }

    @Override
    public String getStateName() { return "PRODUCT_SELECTED"; }
}

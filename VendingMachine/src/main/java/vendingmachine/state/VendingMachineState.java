package vendingmachine.state;

import vendingmachine.TransactionContext;
import vendingmachine.exception.InvalidStateException;
import vendingmachine.model.Coin;
import vendingmachine.payment.PaymentStrategy;

/**
 * Abstract base state for the vending machine FSM.
 *
 * Changed from interface to abstract class because:
 *   - All states share a TransactionContext reference (ctx)
 *   - All states share the reject() helper — no duplication
 *
 * States hold ctx as a field instead of receiving VendingMachine as a method
 * parameter — they only need transaction data, not the full machine API.
 *
 * Default implementations throw InvalidStateException.
 * Subclasses override only the actions valid in that state.
 */
public abstract class VendingMachineState {

    protected final TransactionContext ctx;

    protected VendingMachineState(TransactionContext ctx) {
        this.ctx = ctx;
    }

    public void selectRack(String rackId)                    { reject("selectRack"); }
    public void selectPaymentMethod(PaymentStrategy strategy) { reject("selectPaymentMethod"); }
    public void insertCoin(Coin coin)                        { reject("insertCoin"); }
    public void swipeCard(String cardNumber)                 { reject("swipeCard"); }
    public void cancel()                                     { reject("cancel"); }

    public abstract String getStateName();

    protected void reject(String action) {
        throw new InvalidStateException(action, getStateName());
    }
}

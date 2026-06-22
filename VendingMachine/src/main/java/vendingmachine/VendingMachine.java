package vendingmachine;

import vendingmachine.inventory.Inventory;
import vendingmachine.model.Coin;
import vendingmachine.model.Product;
import vendingmachine.payment.PaymentStrategy;

/**
 * VendingMachine — public API facade. Holds zero transaction state.
 *
 * All mutable state lives in TransactionContext.
 * Every public method is a one-liner delegating to the current FSM state.
 *
 * Singleton via double-checked locking:
 *   - 1st null check (no lock) — fast path, runs on every call after init.
 *   - synchronized block — entered only once, when instance is first created.
 *   - 2nd null check (inside lock) — guards against two threads both passing
 *     the 1st check simultaneously; only one creates the instance.
 *   - volatile — prevents JVM instruction reordering during object construction;
 *     without it a thread could see a non-null but partially constructed instance.
 */
public class VendingMachine {

    // ── Singleton — double-checked locking ───────────────────────────────────

    // volatile: prevents JVM reordering constructor + assignment.
    // Without it: Thread B could see instance != null before constructor finishes.
    private static volatile VendingMachine instance;

    public static VendingMachine getInstance() {
        if (instance == null) {                     // 1st check — no lock (fast path)
            synchronized (VendingMachine.class) {   // lock on Class object (static context)
                if (instance == null) {             // 2nd check — inside lock (safe)
                    instance = new VendingMachine();
                }
            }
        }
        return instance;
    }

    // ── All state lives in TransactionContext ─────────────────────────────────

    private final TransactionContext context;

    private VendingMachine() {
        context = new TransactionContext(new Inventory());
    }

    // ── Inventory setup ───────────────────────────────────────────────────────

    public void addRack(String rackId, Product product, int quantity) {
        context.getInventory().addRack(rackId, product, quantity);
    }

    // ── Public API — pure delegation, zero logic ──────────────────────────────

    public void selectRack(String rackId)                  { context.getState().selectRack(rackId); }
    public void selectPaymentMethod(PaymentStrategy strat) { context.getState().selectPaymentMethod(strat); }
    public void insertCoin(Coin coin)                      { context.getState().insertCoin(coin); }
    public void swipeCard(String cardNumber)               { context.getState().swipeCard(cardNumber); }
    public void cancel()                                   { context.getState().cancel(); }

    // ── Observability ─────────────────────────────────────────────────────────

    public Inventory getInventory()  { return context.getInventory(); }
    public String getCurrentState()  { return context.getState().getStateName(); }
}

package vendingmachine;

import vendingmachine.inventory.Inventory;
import vendingmachine.inventory.Rack;
import vendingmachine.payment.PaymentStrategy;
import vendingmachine.state.IdleState;
import vendingmachine.state.VendingMachineState;

import java.util.concurrent.atomic.AtomicReference;

/**
 * TransactionContext — owns ALL mutable per-transaction state.
 *
 * Extracted from VendingMachine so that:
 *   1. VendingMachine becomes a pure public-API facade with zero state.
 *   2. State classes reference only what they need (ctx), not the full machine.
 *   3. Thread safety is centralized here:
 *      - AtomicReference: state transitions are atomic (CAS prevents two threads
 *        from simultaneously advancing the FSM).
 *      - volatile: per-transaction fields written once, read many times.
 *        Flushes the write to main memory so all threads see the latest value.
 *        NOT sufficient for compound ops (+=) — use AtomicLong for those.
 */
public class TransactionContext {

    private final Inventory inventory;

    // AtomicReference: compareAndSet (CAS) lets us transition states atomically.
    // Two threads in IdleState both calling selectRack: only one wins CAS, other gets false.
    private final AtomicReference<VendingMachineState> currentState = new AtomicReference<>();

    // volatile: written once per transaction by one state, read by the next state.
    // Single writer + multiple readers = volatile is sufficient.
    private volatile Rack            selectedRack;
    private volatile PaymentStrategy paymentStrategy;

    public TransactionContext(Inventory inventory) {
        this.inventory = inventory;
        currentState.set(new IdleState(this));
    }

    // ── State machine ─────────────────────────────────────────────────────────

    public VendingMachineState getState()          { return currentState.get(); }
    public void setState(VendingMachineState next) { currentState.set(next); }

    /**
     * Atomic CAS: only transitions if current state IS expected.
     * Prevents concurrent threads from both advancing the FSM.
     */
    public boolean transition(VendingMachineState expected, VendingMachineState next) {
        return currentState.compareAndSet(expected, next);
    }

    // ── Per-transaction data ──────────────────────────────────────────────────

    public void            setSelectedRack(Rack rack)            { this.selectedRack = rack; }
    public Rack            getSelectedRack()                     { return selectedRack; }

    public void            setPaymentStrategy(PaymentStrategy s) { this.paymentStrategy = s; }
    public PaymentStrategy getPaymentStrategy()                  { return paymentStrategy; }

    public Inventory       getInventory()                        { return inventory; }

    // ── End of transaction — clears all data, returns to Idle ────────────────

    public void reset() {
        selectedRack    = null;
        paymentStrategy = null;
        currentState.set(new IdleState(this));
        display("--- Machine ready ---");
    }

    public void display(String message) {
        System.out.println("[VM] " + message);
    }
}

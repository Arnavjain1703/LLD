package vendingmachine.state;

import vendingmachine.TransactionContext;
import vendingmachine.exception.OutOfStockException;
import vendingmachine.inventory.Rack;

/** IDLE — waiting for rack selection. Only selectRack() and cancel() are valid. */
public class IdleState extends VendingMachineState {

    public IdleState(TransactionContext ctx) {
        super(ctx);
    }

    @Override
    public void selectRack(String rackId) {
        Rack rack = ctx.getInventory().getRack(rackId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid rack: " + rackId));

        if (!rack.isAvailable()) {
            throw new OutOfStockException(rack.getProduct());
        }

        ctx.setSelectedRack(rack);
        ctx.setState(new ProductSelectedState(ctx));
        ctx.display(String.format(
                "Rack %s selected: %s ($%.2f) — choose payment method (CASH / CARD)",
                rackId, rack.getProduct().getName(), rack.getProduct().getPrice()));
    }

    @Override
    public void cancel() {
        ctx.display("Nothing to cancel.");
    }

    @Override
    public String getStateName() { return "IDLE"; }
}

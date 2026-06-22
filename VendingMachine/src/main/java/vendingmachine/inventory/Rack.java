package vendingmachine.inventory;

import vendingmachine.model.Product;

/**
 * Rack — a physical slot in the vending machine (e.g. "A1", "B2").
 * Each rack holds exactly one product type with a quantity counter.
 *
 * deduct() is synchronized: without it two concurrent threads could both
 * pass the quantity > 0 check and both dispense, causing over-dispensing.
 */
public class Rack {

    private final String  rackId;
    private final Product product;
    private int           quantity;

    public Rack(String rackId, Product product, int initialQuantity) {
        if (initialQuantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        this.rackId   = rackId;
        this.product  = product;
        this.quantity = initialQuantity;
    }

    public boolean isAvailable() {
        return quantity > 0;
    }

    /**
     * synchronized: check-then-act must be atomic.
     * Without this, 50 threads on a 5-item rack could all pass isAvailable()
     * and all decrement — dispensing 50 items from a rack of 5.
     */
    public synchronized boolean deduct() {
        if (quantity <= 0) return false;
        quantity--;
        System.out.printf("[RACK %s] Dispensed 1 x %s | Remaining: %d%n",
                rackId, product.getName(), quantity);
        return true;
    }

    public synchronized void restock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Restock quantity must be positive");
        quantity += qty;
        System.out.printf("[RACK %s] Restocked +%d x %s | Total: %d%n",
                rackId, qty, product.getName(), quantity);
    }

    public String  getRackId()              { return rackId; }
    public Product getProduct()             { return product; }
    public synchronized int getQuantity()   { return quantity; }

    @Override
    public String toString() {
        return String.format("Rack[%s -> %s x%d]", rackId, product.getName(), quantity);
    }
}

package model;

import java.util.concurrent.atomic.AtomicInteger;
import exception.OutOfStockException;

public class Rack {
    private final String code;
    private final Product product;
    private final int maxCapacity;
    private final AtomicInteger quantity;

    public Rack(String code, Product product, int initialQuantity, int maxCapacity) {
        if (initialQuantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        if (initialQuantity > maxCapacity) throw new IllegalArgumentException("Quantity exceeds max capacity");
        this.code = code;
        this.product = product;
        this.maxCapacity = maxCapacity;
        this.quantity = new AtomicInteger(initialQuantity);
    }

    public boolean isAvailable() {
        return quantity.get() > 0;
    }

    public void dispense() {
        int current = quantity.get();
        while (current > 0) {
            if (quantity.compareAndSet(current, current - 1)) {
                return;
            }
            current = quantity.get();
        }
        throw new OutOfStockException(code);
    }

    public void restock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Restock amount must be positive");
        int current = quantity.get();
        while (true) {
            int newVal = current + amount;
            if (newVal > maxCapacity) {
                throw new IllegalArgumentException(
                    "Restock of " + amount + " exceeds max capacity " + maxCapacity + " (current: " + current + ")");
            }
            if (quantity.compareAndSet(current, newVal)) {
                return;
            }
            current = quantity.get();
        }
    }

    public String getCode() { return code; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity.get(); }
    public int getMaxCapacity() { return maxCapacity; }
}

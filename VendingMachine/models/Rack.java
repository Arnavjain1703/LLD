package models;

public class Rack {
    private String code;
    private Product product;
    private int quantity;
    private int maxCapacity;

    public Rack(String code, Product product, int quantity, int maxCapacity) {
        this.code = code;
        this.product = product;
        this.quantity = quantity;
        this.maxCapacity = maxCapacity;
    }

    public synchronized boolean isAvailable() {
        return quantity > 0;
    }

    public synchronized void dispense() {
        if (quantity <= 0) {
            throw new RuntimeException("Rack " + code + " is empty");
        }
        quantity--;
    }

    public synchronized void restock(int amount) {
        if (quantity + amount > maxCapacity) {
            throw new RuntimeException("Restock exceeds max capacity of " + maxCapacity);
        }
        quantity += amount;
    }

    public Product getProduct() {
        return product;
    }

    public synchronized int getQuantity() {
        return quantity;
    }

    public String getCode() {
        return code;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}

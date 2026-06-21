package model;

public final class Product {
    private final String code;
    private final String name;
    private final double price;

    public Product(String code, String name, double price) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Code cannot be blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        if (price <= 0) throw new IllegalArgumentException("Price must be positive");
        this.code = code;
        this.name = name;
        this.price = price;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    @Override
    public String toString() {
        return String.format("%s ($%.2f)", name, price);
    }
}

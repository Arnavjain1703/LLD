package vendingmachine.model;

import java.util.Objects;

public class Product {
    private final String productId;
    private final String name;
    // Store price in cents to avoid floating-point precision issues
    private final long priceInCents;

    public Product(String productId, String name, long priceInCents) {
        if (priceInCents <= 0) throw new IllegalArgumentException("Price must be positive");
        this.productId    = productId;
        this.name         = name;
        this.priceInCents = priceInCents;
    }

    public String getProductId()    { return productId; }
    public String getName()         { return name; }
    public long   getPriceInCents() { return priceInCents; }
    public double getPrice()        { return priceInCents / 100.0; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        return Objects.equals(productId, ((Product) o).productId);
    }
    @Override public int    hashCode() { return Objects.hash(productId); }
    @Override public String toString() { return String.format("Product[%s, $%.2f]", name, getPrice()); }
}
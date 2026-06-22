package vendingmachine.exception;

import vendingmachine.model.Product;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(Product product) {
        super(String.format("Product [%s] is out of stock", product.getName()));
    }
}

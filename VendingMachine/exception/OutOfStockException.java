package exception;

public class OutOfStockException extends VendingMachineException {
    public OutOfStockException(String code) {
        super("Product " + code + " is out of stock");
    }
}

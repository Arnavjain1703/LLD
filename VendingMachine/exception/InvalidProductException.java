package exception;

public class InvalidProductException extends VendingMachineException {
    public InvalidProductException(String code) {
        super("Invalid product code: " + code);
    }
}

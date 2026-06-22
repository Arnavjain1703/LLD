package vendingmachine.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(double required, double inserted) {
        super(String.format("Insufficient funds: need $%.2f, inserted $%.2f", required, inserted));
    }
}

package exception;

public class InsufficientFundsException extends VendingMachineException {
    public InsufficientFundsException(double required, double available) {
        super(String.format("Insufficient funds. Need: $%.2f | Have: $%.2f", required, available));
    }
}

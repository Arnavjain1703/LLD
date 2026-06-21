package exception;

public class PaymentFailedException extends VendingMachineException {
    public PaymentFailedException() {
        super("Payment processing failed");
    }
}

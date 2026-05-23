package models.PaymentProcessor;

public class CashPaymentProcessor implements PaymentProcessorInterface {

    @Override
    public boolean process(double amount) {
        System.out.println("Processing cash payment of: " + amount);
        // In real world: interact with cash register, validate amount, give change
        return true;
    }
}

package models.PaymentProcessor;

public class CardPaymentProcessor implements PaymentProcessorInterface {

    @Override
    public boolean process(double amount) {
        System.out.println("Processing card payment of: " + amount);
        // In real world: call payment gateway, validate card, process transaction
        return true;
    }
}

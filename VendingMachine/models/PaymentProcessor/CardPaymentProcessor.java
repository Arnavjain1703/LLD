package models.PaymentProcessor;

public class CardPaymentProcessor implements PaymentProcessorInterface {

    @Override
    public boolean process(double amount) {
        System.out.println("Processing card payment of: " + amount);
        return true;
    }

    @Override
    public boolean refund(double amount) {
        System.out.println("Refunding to card: " + amount);
        return true;
    }
}

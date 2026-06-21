package models.PaymentProcessor;

public class CashPaymentProcessor implements PaymentProcessorInterface {

    @Override
    public boolean process(double amount) {
        System.out.println("Processing cash payment of: " + amount);
        return true;
    }

    @Override
    public boolean refund(double amount) {
        System.out.println("Refunding cash: " + amount);
        return true;
    }
}

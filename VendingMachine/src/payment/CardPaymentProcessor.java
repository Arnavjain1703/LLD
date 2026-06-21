package payment;

public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean charge(double amount) {
        System.out.printf("  [PAYMENT] Charged $%.2f (card)%n", amount);
        return true;
    }

    @Override
    public boolean refund(double amount) {
        System.out.printf("  [PAYMENT] Refunded $%.2f (card reversal)%n", amount);
        return true;
    }
}

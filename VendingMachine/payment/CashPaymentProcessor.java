package payment;

public class CashPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean charge(double amount) {
        System.out.printf("  [PAYMENT] Charged $%.2f (cash)%n", amount);
        return true;
    }

    @Override
    public boolean refund(double amount) {
        System.out.printf("  [PAYMENT] Refunded $%.2f (cash dispensed)%n", amount);
        return true;
    }
}

package payment;

public interface PaymentProcessor {
    boolean charge(double amount);
    boolean refund(double amount);
}

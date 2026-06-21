package models.PaymentProcessor;

public interface PaymentProcessorInterface {
    boolean process(double amount);
    boolean refund(double amount);
}

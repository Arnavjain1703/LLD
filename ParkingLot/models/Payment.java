package models;

import enums.PaymentMethod;
import enums.PaymentStatus;
import models.PaymentProcessor.PaymentProcessorInterface;

public class Payment {
    private static int counter = 0;
    private String paymentId;
    private Ticket ticket;
    private double amount;
    private PaymentMethod method;
    private PaymentStatus status;

    public Payment(Ticket ticket, double amount, PaymentMethod method) {
        this.paymentId = "PAY-" + (++counter);
        this.ticket = ticket;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
    }

    public boolean processPayment(PaymentProcessorInterface processor) {
        boolean success = processor.process(amount);
        if (success) {
            this.status = PaymentStatus.COMPLETED;
            System.out.println("Payment " + paymentId + " completed. Amount: " + amount);
        } else {
            this.status = PaymentStatus.FAILED;
            System.out.println("Payment " + paymentId + " failed.");
        }
        return success;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Ticket getTicket() {
        return ticket;
    }
}

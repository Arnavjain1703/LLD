package models;

import java.time.LocalDateTime;

import enums.PaymentMethod;
import enums.TicketStatus;
import models.FeeCalculator.FeeCalculatorInterface;
import models.PaymentProcessor.PaymentProcessorInterface;

public class ExitGate {
    private String gateId;
    private FeeCalculatorInterface feeCalculator;

    public ExitGate(String gateId, FeeCalculatorInterface feeCalculator) {
        this.gateId = gateId;
        this.feeCalculator = feeCalculator;
    }

    /**
     * Processes vehicle exit:
     * 1. Sets exit time on ticket
     * 2. Calculates fee using the fee strategy
     * 3. Creates a Payment and processes it
     * 4. Frees the parking spot
     */
    public Payment processExit(Ticket ticket, PaymentMethod paymentMethod,
                               PaymentProcessorInterface processor) {
        // Set exit time
        ticket.setExitTime(LocalDateTime.now());

        // Calculate fee
        double fee = feeCalculator.calculate(ticket);
        System.out.println("Gate " + gateId + " — Fee calculated: " + fee);

        // Create and process payment
        Payment payment = new Payment(ticket, fee, paymentMethod);
        boolean success = payment.processPayment(processor);

        if (success) {
            // Free the spot
            ticket.getSpot().unpark();
            ticket.setStatus(TicketStatus.PAID);
            System.out.println("Vehicle exited. Spot " + ticket.getSpot().getSpotId() + " is now free.");
        }

        return payment;
    }

    public String getGateId() {
        return gateId;
    }
}

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

    public Payment processExit(Ticket ticket, PaymentMethod paymentMethod,
                               PaymentProcessorInterface processor) {
        ticket.setExitTime(LocalDateTime.now());

        double fee = feeCalculator.calculate(ticket);
        System.out.println("Gate " + gateId + " — Fee calculated: " + fee);

        Payment payment = new Payment(ticket, fee, paymentMethod);
        boolean success = payment.processPayment(processor);

        if (success) {
            ticket.getSpot().unpark();
            ticket.getFloor().unparkVehicle(ticket.getSpot());
            ticket.setStatus(TicketStatus.PAID);
            System.out.println("Vehicle exited. Spot " + ticket.getSpot().getSpotId() + " is now free.");
        }

        return payment;
    }

    public String getGateId() {
        return gateId;
    }
}

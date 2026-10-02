package model;

import java.time.LocalDateTime;

/**
 * Invoice generated at checkout — summarises the stay and payment.
 */
public class Invoice {
    private final String        invoiceId;
    private final Booking       booking;
    private final Payment       payment;
    private final LocalDateTime generatedAt;

    public Invoice(String invoiceId, Booking booking, Payment payment) {
        this.invoiceId   = invoiceId;
        this.booking     = booking;
        this.payment     = payment;
        this.generatedAt = LocalDateTime.now();
    }

    public String        getInvoiceId()   { return invoiceId; }
    public Booking       getBooking()     { return booking; }
    public Payment       getPayment()     { return payment; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }

    @Override
    public String toString() {
        return "\n===== INVOICE =====" +
               "\nInvoice ID  : " + invoiceId +
               "\nBooking     : " + booking.getBookingId() +
               "\nRoom        : " + booking.getRoomId() +
               "\nCheck-in    : " + booking.getCheckIn() +
               "\nCheck-out   : " + booking.getCheckOut() +
               "\nNights      : " + booking.getNights() +
               "\nTotal       : " + booking.getTotalAmount() +
               "\nPaid via    : " + payment.getMethod() +
               "\nGenerated   : " + generatedAt +
               "\n===================";
    }
}

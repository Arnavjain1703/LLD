package com.hotelmanagement.payment;

import com.hotelmanagement.enums.PaymentMethod;
import com.hotelmanagement.models.Booking;
import com.hotelmanagement.models.Payment;

public interface PaymentService {
    Payment processPayment(Booking booking, PaymentMethod method);
    Payment processRefund(Booking booking);
    double calculateTotal(Booking booking, double basePricePerNight);
}

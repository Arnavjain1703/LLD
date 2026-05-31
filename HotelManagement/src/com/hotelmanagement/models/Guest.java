package com.hotelmanagement.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Guest {
    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    private final String idProof;
    private final List<String> bookingHistory; // bookingIds

    public Guest(String id, String name, String email, String phone, String idProof) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.idProof = idProof;
        this.bookingHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public void addBooking(String bookingId) { bookingHistory.add(bookingId); }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getIdProof() { return idProof; }
    public List<String> getBookingHistory() { return List.copyOf(bookingHistory); }

    @Override
    public String toString() {
        return String.format("Guest[%s | %s | %s]", name, email, phone);
    }
}

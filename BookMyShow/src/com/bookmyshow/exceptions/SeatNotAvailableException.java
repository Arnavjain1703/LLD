package com.bookmyshow.exceptions;

public class SeatNotAvailableException extends RuntimeException {
    public SeatNotAvailableException(String msg) { super(msg); }
}

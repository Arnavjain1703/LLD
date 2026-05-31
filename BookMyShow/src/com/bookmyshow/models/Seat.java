package com.bookmyshow.models;

import com.bookmyshow.enums.SeatType;

public class Seat {
    private final String   id;
    private final int      row;
    private final int      col;
    private final SeatType type;
    private final double   basePrice;

    public Seat(String id, int row, int col, SeatType type, double basePrice) {
        this.id = id; this.row = row; this.col = col;
        this.type = type; this.basePrice = basePrice;
    }

    public String   getId()        { return id; }
    public int      getRow()       { return row; }
    public int      getCol()       { return col; }
    public SeatType getType()      { return type; }
    public double   getBasePrice() { return basePrice; }
}

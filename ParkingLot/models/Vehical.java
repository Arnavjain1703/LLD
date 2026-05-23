package models;

import enums.vechicle;

public class Vehical {
    private int vehicalNumber;
    private vechicle vehicalType;

    public Vehical(int vehicalNumber, vechicle vehicalType) {
        this.vehicalNumber = vehicalNumber;
        this.vehicalType = vehicalType;
    }

    public vechicle getVehicalType() {
        return vehicalType;
    }

    public int getVehicalNumber() {
        return vehicalNumber;
    }
}
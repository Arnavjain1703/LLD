package vendingmachine.model;

public enum Coin {
    PENNY(1),
    NICKEL(5),
    DIME(10),
    QUARTER(25),
    ONE(100),
    FIVE(500);

    private final long valueInCents;

    Coin(long valueInCents) { this.valueInCents = valueInCents; }

    public long   getValueInCents() { return valueInCents; }
    public double getValue()        { return valueInCents / 100.0; }

    @Override public String toString() { return String.format("%s($%.2f)", name(), getValue()); }
}
package models.ItemDispenseStrategy;

import models.Rack;

public class StandardDispenseStrategy implements ItemDispenseStrategy {

    @Override
    public void dispense(Rack rack) {
        rack.dispense();
        System.out.println("Item dispensed from rack " + rack.getCode());
    }
}

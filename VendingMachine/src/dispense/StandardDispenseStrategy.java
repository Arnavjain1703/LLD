package dispense;

import model.Rack;

public class StandardDispenseStrategy implements DispenseStrategy {

    @Override
    public void dispense(Rack rack) {
        rack.dispense();
        System.out.println("  [DISPENSE] " + rack.getProduct().getName() + " dropped from rack " + rack.getCode());
    }
}

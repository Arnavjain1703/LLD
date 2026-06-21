package state;

import exception.IllegalStateTransitionException;
import model.Product;
import model.VendingMachine;

public class IdleState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        if (!machine.getInventory().isAvailable(code)) {
            machine.getDisplay().showError("Product " + code + " is out of stock.");
            return;
        }
        Product product = machine.getInventory().getProduct(code);
        machine.setSelectedProduct(product);
        machine.getDisplay().showMessage("Selected: " + product.getName()
                + " | Price: $" + String.format("%.2f", product.getPrice()));
        machine.getDisplay().showInsertMoney(0, product.getPrice());
        machine.setState(new HasMoneyState());
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        throw new IllegalStateTransitionException("insert money", getName());
    }

    @Override
    public void dispense(VendingMachine machine) {
        throw new IllegalStateTransitionException("dispense", getName());
    }

    @Override
    public void cancel(VendingMachine machine) {
        machine.getDisplay().showMessage("No transaction in progress.");
    }

    @Override
    public String getName() { return "IDLE"; }
}

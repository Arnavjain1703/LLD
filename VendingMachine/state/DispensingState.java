package state;

import exception.IllegalStateTransitionException;
import model.Product;
import model.Rack;
import model.VendingMachine;

public class DispensingState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        throw new IllegalStateTransitionException("select product", getName());
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        throw new IllegalStateTransitionException("insert money", getName());
    }

    @Override
    public void dispense(VendingMachine machine) {
        Product product = machine.getSelectedProduct();
        double price = product.getPrice();

        Rack rack = machine.getInventory().getRack(product.getCode());
        machine.getDispenseStrategy().dispense(rack);

        double change = machine.getBalance() - price;
        if (change > 0) {
            machine.getPaymentProcessor().refund(change);
            machine.getDisplay().showChange(change);
        }

        machine.getDisplay().showDispenseSuccess(product.getName());
        machine.resetTransaction();
    }

    @Override
    public void cancel(VendingMachine machine) {
        throw new IllegalStateTransitionException("cancel", getName());
    }

    @Override
    public String getName() { return "DISPENSING"; }
}

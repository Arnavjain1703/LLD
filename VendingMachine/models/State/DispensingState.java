package models.State;

import models.Rack;
import models.VendingMachine;

public class DispensingState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        machine.getDisplay().showError("Dispensing in progress. Please wait.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.getDisplay().showError("Dispensing in progress. Please wait.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        String code = machine.getSelectedProduct().getCode();
        double price = machine.getSelectedProduct().getPrice();

        Rack rack = machine.getInventory().getRack(code);
        machine.getDispenseStrategy().dispense(rack);
        machine.getDisplay().showDispensing(machine.getSelectedProduct().getName());

        double change = machine.getBalance() - price;
        if (change > 0) {
            machine.getPaymentProcessor().refund(change);
            machine.getDisplay().showChange(change);
        }

        machine.resetBalance();
        machine.setSelectedProduct(null);
        machine.setState(new IdleState());
        machine.getDisplay().showWelcome();
    }

    @Override
    public void cancel(VendingMachine machine) {
        machine.getDisplay().showError("Cannot cancel. Dispensing in progress.");
    }
}

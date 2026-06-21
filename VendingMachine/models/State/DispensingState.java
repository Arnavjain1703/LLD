package models.State;

import models.Rack;
import models.User;
import models.VendingMachine;

public class DispensingState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, User user, String code) {
        machine.getDisplay().showError("Dispensing in progress. Please wait.");
    }

    @Override
    public void insertMoney(VendingMachine machine, User user, double amount) {
        machine.getDisplay().showError("Dispensing in progress. Please wait.");
    }

    @Override
    public void dispense(VendingMachine machine, User user) {
        String code = user.getSelectedProduct().getCode();
        double price = user.getSelectedProduct().getPrice();

        Rack rack = machine.getInventory().getRack(code);
        machine.getDispenseStrategy().dispense(rack);
        machine.getDisplay().showDispensing(user.getSelectedProduct().getName());

        double change = user.getBalance() - price;
        if (change > 0) {
            machine.getPaymentProcessor().refund(change);
            machine.getDisplay().showChange(change);
        }

        user.resetBalance();
        user.setSelectedProduct(null);
        user.setState(new IdleState());
        machine.getDisplay().showWelcome();
    }

    @Override
    public void cancel(VendingMachine machine, User user) {
        machine.getDisplay().showError("Cannot cancel. Dispensing in progress.");
    }
}

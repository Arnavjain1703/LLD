package models.State;

import models.VendingMachine;

public class IdleState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        if (!machine.getInventory().isAvailable(code)) {
            machine.getDisplay().showError("Product " + code + " is out of stock.");
            machine.getDisplay().showWelcome();
            return;
        }
        machine.setSelectedProduct(machine.getInventory().getProduct(code));
        machine.getDisplay().showMessage("Selected: " + machine.getSelectedProduct().getName()
                + " | Price: $" + String.format("%.2f", machine.getSelectedProduct().getPrice()));
        machine.getDisplay().showInsertMoney(0, machine.getSelectedProduct().getPrice());
        machine.setState(new HasMoneyState());
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.getDisplay().showError("Please select a product first.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        machine.getDisplay().showError("Please select a product and insert money first.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        machine.getDisplay().showMessage("No transaction in progress.");
    }
}

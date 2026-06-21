package models.State;

import models.User;
import models.VendingMachine;

public class IdleState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, User user, String code) {
        if (!machine.getInventory().isAvailable(code)) {
            machine.getDisplay().showError("Product " + code + " is out of stock.");
            machine.getDisplay().showWelcome();
            return;
        }
        user.setSelectedProduct(machine.getInventory().getProduct(code));
        machine.getDisplay().showMessage("Selected: " + user.getSelectedProduct().getName()
                + " | Price: $" + String.format("%.2f", user.getSelectedProduct().getPrice()));
        machine.getDisplay().showInsertMoney(0, user.getSelectedProduct().getPrice());
        user.setState(new HasMoneyState());
    }

    @Override
    public void insertMoney(VendingMachine machine, User user, double amount) {
        machine.getDisplay().showError("Please select a product first.");
    }

    @Override
    public void dispense(VendingMachine machine, User user) {
        machine.getDisplay().showError("Please select a product and insert money first.");
    }

    @Override
    public void cancel(VendingMachine machine, User user) {
        machine.getDisplay().showMessage("No transaction in progress.");
    }
}

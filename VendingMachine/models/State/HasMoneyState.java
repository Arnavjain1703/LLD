package models.State;

import models.User;
import models.VendingMachine;

public class HasMoneyState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, User user, String code) {
        machine.getDisplay().showError("Product already selected. Complete or cancel current user.");
    }

    @Override
    public void insertMoney(VendingMachine machine, User user, double amount) {
        user.addBalance(amount);
        machine.getDisplay().showInsertMoney(user.getBalance(), user.getSelectedProduct().getPrice());

        if (user.getBalance() >= user.getSelectedProduct().getPrice()) {
            machine.getDisplay().showMessage("Sufficient funds. Press DISPENSE to collect your item.");
        }
    }

    @Override
    public void dispense(VendingMachine machine, User user) {
        double price = user.getSelectedProduct().getPrice();

        if (user.getBalance() < price) {
            machine.getDisplay().showError("Insufficient funds. Need: $"
                    + String.format("%.2f", price) + " | Have: $"
                    + String.format("%.2f", user.getBalance()));
            machine.getDisplay().showMessage("Insert more money or press CANCEL.");
            return;
        }

        if (!machine.getPaymentProcessor().process(price)) {
            machine.getDisplay().showError("Payment processing failed. Try again.");
            return;
        }

        user.setState(new DispensingState());
        machine.dispense();
    }

    @Override
    public void cancel(VendingMachine machine, User user) {
        double refund = user.getBalance();
        if (refund > 0) {
            machine.getPaymentProcessor().refund(refund);
        }
        user.resetBalance();
        user.setSelectedProduct(null);
        user.setState(new IdleState());
        machine.getDisplay().showRefund(refund);
        machine.getDisplay().showWelcome();
    }
}

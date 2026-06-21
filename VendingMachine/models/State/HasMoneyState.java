package models.State;

import models.VendingMachine;

public class HasMoneyState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        machine.getDisplay().showError("Product already selected. Complete or cancel current transaction.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.addBalance(amount);
        machine.getDisplay().showInsertMoney(machine.getBalance(), machine.getSelectedProduct().getPrice());

        if (machine.getBalance() >= machine.getSelectedProduct().getPrice()) {
            machine.getDisplay().showMessage("Sufficient funds. Press DISPENSE to collect your item.");
        }
    }

    @Override
    public void dispense(VendingMachine machine) {
        double price = machine.getSelectedProduct().getPrice();

        if (machine.getBalance() < price) {
            machine.getDisplay().showError("Insufficient funds. Need: $"
                    + String.format("%.2f", price) + " | Have: $"
                    + String.format("%.2f", machine.getBalance()));
            machine.getDisplay().showMessage("Insert more money or press CANCEL.");
            return;
        }

        if (!machine.getPaymentProcessor().process(price)) {
            machine.getDisplay().showError("Payment processing failed. Try again.");
            return;
        }

        machine.setState(new DispensingState());
        machine.dispense();
    }

    @Override
    public void cancel(VendingMachine machine) {
        double refund = machine.getBalance();
        if (refund > 0) {
            machine.getPaymentProcessor().refund(refund);
        }
        machine.resetBalance();
        machine.setSelectedProduct(null);
        machine.setState(new IdleState());
        machine.getDisplay().showRefund(refund);
        machine.getDisplay().showWelcome();
    }
}

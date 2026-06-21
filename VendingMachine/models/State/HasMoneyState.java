package models.State;

import models.VendingMachine;

public class HasMoneyState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        System.out.println("Product already selected. Complete or cancel current transaction.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.addBalance(amount);
        System.out.println("Inserted: " + amount + " | Balance: " + machine.getBalance());
    }

    @Override
    public void dispense(VendingMachine machine) {
        double price = machine.getSelectedProduct().getPrice();

        if (machine.getBalance() < price) {
            System.out.println("Insufficient funds. Need: " + price
                    + " | Have: " + machine.getBalance());
            System.out.println("Insert more money or cancel.");
            return;
        }

        machine.setState(new DispensingState());
        machine.dispense();
    }

    @Override
    public void cancel(VendingMachine machine) {
        double refund = machine.getBalance();
        machine.resetBalance();
        machine.setSelectedProduct(null);
        machine.setState(new IdleState());
        System.out.println("Transaction cancelled. Refunded: " + refund);
    }
}

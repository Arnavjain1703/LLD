package models.State;

import models.VendingMachine;

public class IdleState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        if (!machine.getInventory().isAvailable(code)) {
            System.out.println("Product " + code + " is out of stock.");
            return;
        }
        machine.setSelectedProduct(machine.getInventory().getProduct(code));
        System.out.println("Selected: " + machine.getSelectedProduct().getName()
                + " | Price: " + machine.getSelectedProduct().getPrice());
        System.out.println("Please insert money.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        if (machine.getSelectedProduct() == null) {
            System.out.println("Please select a product first.");
            return;
        }
        machine.addBalance(amount);
        System.out.println("Inserted: " + amount + " | Balance: " + machine.getBalance());
        machine.setState(new HasMoneyState());
    }

    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("Please select a product and insert money first.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("No transaction in progress.");
    }
}

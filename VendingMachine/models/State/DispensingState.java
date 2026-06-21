package models.State;

import models.VendingMachine;

public class DispensingState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        System.out.println("Dispensing in progress. Please wait.");
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        System.out.println("Dispensing in progress. Please wait.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        String code = machine.getSelectedProduct().getCode();
        double price = machine.getSelectedProduct().getPrice();

        machine.getInventory().dispenseItem(code);
        System.out.println("Dispensed: " + machine.getSelectedProduct().getName());

        double change = machine.getBalance() - price;
        if (change > 0) {
            System.out.println("Returning change: " + change);
        }

        machine.resetBalance();
        machine.setSelectedProduct(null);
        machine.setState(new IdleState());
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Cannot cancel. Dispensing in progress.");
    }
}

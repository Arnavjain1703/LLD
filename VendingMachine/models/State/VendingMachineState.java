package models.State;

import models.VendingMachine;

public interface VendingMachineState {
    void selectProduct(VendingMachine machine, String code);
    void insertMoney(VendingMachine machine, double amount);
    void dispense(VendingMachine machine);
    void cancel(VendingMachine machine);
}

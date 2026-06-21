package models.State;

import models.User;
import models.VendingMachine;

public interface VendingMachineState {
    void selectProduct(VendingMachine machine, User transaction, String code);
    void insertMoney(VendingMachine machine, User transaction, double amount);
    void dispense(VendingMachine machine, User transaction);
    void cancel(VendingMachine machine, User transaction);
}

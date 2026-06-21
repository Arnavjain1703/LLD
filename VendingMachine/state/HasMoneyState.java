package state;

import exception.IllegalStateTransitionException;
import exception.InsufficientFundsException;
import exception.PaymentFailedException;
import model.Product;
import model.VendingMachine;

public class HasMoneyState implements VendingMachineState {

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        throw new IllegalStateTransitionException("select product", getName());
    }

    @Override
    public void insertMoney(VendingMachine machine, double amount) {
        machine.addBalance(amount);
        Product product = machine.getSelectedProduct();
        machine.getDisplay().showInsertMoney(machine.getBalance(), product.getPrice());

        if (machine.getBalance() >= product.getPrice()) {
            machine.getDisplay().showMessage("Sufficient funds. Press DISPENSE to collect your item.");
        }
    }

    @Override
    public void dispense(VendingMachine machine) {
        Product product = machine.getSelectedProduct();
        double price = product.getPrice();

        if (machine.getBalance() < price) {
            throw new InsufficientFundsException(price, machine.getBalance());
        }

        if (!machine.getPaymentProcessor().charge(price)) {
            throw new PaymentFailedException();
        }

        machine.setState(new DispensingState());
        machine.getState().dispense(machine);
    }

    @Override
    public void cancel(VendingMachine machine) {
        double refund = machine.getBalance();
        if (refund > 0) {
            machine.getPaymentProcessor().refund(refund);
        }
        machine.resetTransaction();
        machine.getDisplay().showRefund(refund);
    }

    @Override
    public String getName() { return "HAS_MONEY"; }
}

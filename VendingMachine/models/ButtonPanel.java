package models;

public class ButtonPanel {
    private final VendingMachine machine;

    public ButtonPanel(VendingMachine machine) {
        this.machine = machine;
    }

    public void pressProductButton(String code) {
        System.out.println("\n[BUTTON] Product selected: " + code);
        machine.selectProduct(code);
    }

    public void pressInsertCash(double amount) {
        System.out.printf("[BUTTON] Cash inserted: $%.2f%n", amount);
        machine.insertMoney(amount);
    }

    public void pressDispense() {
        System.out.println("[BUTTON] Dispense pressed");
        machine.dispense();
    }

    public void pressCancel() {
        System.out.println("[BUTTON] Cancel pressed");
        machine.cancelTransaction();
    }

    public void pressDisplayProducts() {
        System.out.println("[BUTTON] Display products pressed");
        machine.displayProducts();
    }
}

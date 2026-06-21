package model;

import java.util.concurrent.ConcurrentHashMap;

public class ButtonPanel {
    private final VendingMachine machine;
    private final ConcurrentHashMap<String, Button> productButtons = new ConcurrentHashMap<>();
    private final Button dispenseButton;
    private final Button cancelButton;
    private final Button displayButton;

    public ButtonPanel(VendingMachine machine) {
        this.machine = machine;
        this.dispenseButton = new Button("DISPENSE", machine::dispense);
        this.cancelButton = new Button("CANCEL", machine::cancelTransaction);
        this.displayButton = new Button("DISPLAY", () -> machine.displayProducts());
    }

    public void addProductButton(String code) {
        productButtons.put(code, new Button("SELECT " + code, () -> machine.selectProduct(code)));
    }

    public void removeProductButton(String code) {
        productButtons.remove(code);
    }

    public void pressProductButton(String code) {
        Button button = productButtons.get(code);
        if (button == null) {
            System.out.println("  [ERROR] No button for code: " + code);
            return;
        }
        button.press();
    }

    public void pressInsertCash(double amount) {
        System.out.printf("%n[COIN SLOT] Inserted $%.2f%n", amount);
        machine.insertMoney(amount);
    }

    public void pressDispense() {
        dispenseButton.press();
    }

    public void pressCancel() {
        cancelButton.press();
    }

    public void pressDisplayProducts() {
        displayButton.press();
    }
}

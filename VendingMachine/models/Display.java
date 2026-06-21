package models;

import java.util.Map;

public class Display {
    private String currentMessage;

    public Display() {
        this.currentMessage = "Welcome! Please select a product.";
    }

    public void showWelcome() {
        currentMessage = "Welcome! Please select a product.";
        render();
    }

    public void showProducts(Inventory inventory) {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║         AVAILABLE PRODUCTS             ║");
        System.out.println("╠════════════════════════════════════════╣");

        Map<String, Integer> report = inventory.getReport();
        for (Map.Entry<String, Rack> entry : inventory.getAllRacks().entrySet()) {
            String code = entry.getKey();
            Rack rack = entry.getValue();
            String status = rack.isAvailable() ? String.valueOf(rack.getQuantity()) : "SOLD OUT";
            System.out.printf("║  [%s] %-20s $%.2f  (%s) ║%n",
                    code, rack.getProduct().getName(), rack.getProduct().getPrice(), status);
        }

        System.out.println("╚════════════════════════════════════════╝");
    }

    public void showMessage(String message) {
        this.currentMessage = message;
        render();
    }

    public void showInsertMoney(double balance, double required) {
        System.out.println("┌────────────────────────────────────────┐");
        System.out.printf("│  Balance: $%.2f / Required: $%.2f     │%n", balance, required);
        System.out.println("│  Insert money or press CANCEL          │");
        System.out.println("└────────────────────────────────────────┘");
    }

    public void showDispensing(String productName) {
        System.out.println("┌────────────────────────────────────────┐");
        System.out.println("│  *** DISPENSING: " + productName + " ***");
        System.out.println("│  Please collect your item below.       │");
        System.out.println("└────────────────────────────────────────┘");
    }

    public void showChange(double amount) {
        if (amount > 0) {
            System.out.printf("  >> Change returned: $%.2f%n", amount);
        }
    }

    public void showRefund(double amount) {
        System.out.printf("  >> Transaction cancelled. Refunded: $%.2f%n", amount);
    }

    public void showError(String error) {
        System.out.println("  [ERROR] " + error);
    }

    private void render() {
        System.out.println("  >> " + currentMessage);
    }
}

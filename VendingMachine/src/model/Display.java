package model;

import java.util.Map;

public class Display {

    public void showWelcome() {
        System.out.println("  >> Welcome! Please select a product.");
    }

    public void showProducts(Inventory inventory) {
        Map<String, Rack> racks = inventory.getAllRacks();
        System.out.println("\n  ╔════════════════════════════════════════╗");
        System.out.println("  ║         AVAILABLE PRODUCTS             ║");
        System.out.println("  ╠════════════════════════════════════════╣");
        for (Map.Entry<String, Rack> entry : racks.entrySet()) {
            Rack rack = entry.getValue();
            String status = rack.isAvailable() ? String.valueOf(rack.getQuantity()) : "SOLD OUT";
            System.out.printf("  ║  [%s] %-18s $%.2f  (%s)%n",
                    rack.getCode(), rack.getProduct().getName(), rack.getProduct().getPrice(), status);
        }
        System.out.println("  ╚════════════════════════════════════════╝");
    }

    public void showMessage(String message) {
        System.out.println("  >> " + message);
    }

    public void showInsertMoney(double balance, double required) {
        System.out.printf("  >> Balance: $%.2f / Required: $%.2f%n", balance, required);
    }

    public void showDispenseSuccess(String productName) {
        System.out.println("  >> *** DISPENSED: " + productName + " *** Collect your item.");
    }

    public void showChange(double amount) {
        System.out.printf("  >> Change returned: $%.2f%n", amount);
    }

    public void showRefund(double amount) {
        System.out.printf("  >> Transaction cancelled. Refunded: $%.2f%n", amount);
    }

    public void showError(String error) {
        System.out.println("  [ERROR] " + error);
    }
}

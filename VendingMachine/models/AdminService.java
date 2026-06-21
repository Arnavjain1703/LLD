package models;

import java.util.Map;

public class AdminService {
    private VendingMachine machine;

    public AdminService(VendingMachine machine) {
        this.machine = machine;
    }

    public void addProduct(String code, Product product, int quantity, int maxCapacity) {
        Rack rack = new Rack(code, product, quantity, maxCapacity);
        machine.getInventory().addRack(rack);
        System.out.println("Added product: " + product.getName() + " at slot " + code);
    }

    public void removeProduct(String code) {
        machine.getInventory().removeRack(code);
        System.out.println("Removed product at slot: " + code);
    }

    public void restock(String code, int quantity) {
        machine.getInventory().restock(code, quantity);
        System.out.println("Restocked slot " + code + " with " + quantity + " items.");
    }

    public void getInventoryReport() {
        Map<String, Integer> report = machine.getInventory().getReport();
        System.out.println("\n--- Inventory Report ---");
        for (Map.Entry<String, Integer> entry : report.entrySet()) {
            System.out.println("  " + entry.getKey() + " : " + entry.getValue() + " items");
        }
        System.out.println("------------------------\n");
    }
}

package model;

import java.util.Map;

public class AdminService {
    private final VendingMachine machine;

    public AdminService(VendingMachine machine) {
        this.machine = machine;
    }

    public void addProduct(String code, Product product, int quantity, int maxCapacity) {
        Rack rack = new Rack(code, product, quantity, maxCapacity);
        machine.getInventory().addRack(rack);
        machine.getButtonPanel().addProductButton(code);
        System.out.println("[ADMIN] Added: " + product.getName() + " at slot " + code
                + " (qty: " + quantity + ", max: " + maxCapacity + ")");
    }

    public void removeProduct(String code) {
        machine.getInventory().removeRack(code);
        machine.getButtonPanel().removeProductButton(code);
        System.out.println("[ADMIN] Removed slot: " + code);
    }

    public void restock(String code, int quantity) {
        machine.getInventory().getRack(code).restock(quantity);
        System.out.println("[ADMIN] Restocked " + code + " with " + quantity + " items");
    }

    public void printInventoryReport() {
        Map<String, Rack> racks = machine.getInventory().getAllRacks();
        System.out.println("\n  ┌─── Inventory Report ───────────────────┐");
        for (Map.Entry<String, Rack> entry : racks.entrySet()) {
            Rack rack = entry.getValue();
            System.out.printf("  │  [%s] %-18s %d / %d%n",
                    rack.getCode(), rack.getProduct().getName(), rack.getQuantity(), rack.getMaxCapacity());
        }
        System.out.println("  └─────────────────────────────────────────┘");
    }
}

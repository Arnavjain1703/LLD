package vendingmachine.inventory;

import vendingmachine.model.Product;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Inventory — manages racks by rackId.
 * Direct O(1) lookup by rackId — no product search needed.
 * Plain HashMap is sufficient: restocking is a physical/offline operation.
 */
public class Inventory {

    private final Map<String, Rack> racks = new HashMap<>();

    public void addRack(Rack rack) {
        racks.put(rack.getRackId(), rack);
        System.out.printf("[INVENTORY] Added %s%n", rack);
    }

    public void addRack(String rackId, Product product, int quantity) {
        addRack(new Rack(rackId, product, quantity));
    }

    /** Direct availability check by rackId — O(1). */
    public boolean isAvailable(String rackId) {
        Rack rack = racks.get(rackId);
        return rack != null && rack.isAvailable();
    }

    /** Direct deduction by rackId — O(1). */
    public boolean deduct(String rackId) {
        Rack rack = racks.get(rackId);
        if (rack == null) return false;
        return rack.deduct();
    }

    public void restock(String rackId, int qty) {
        Rack rack = racks.get(rackId);
        if (rack == null) throw new IllegalArgumentException("Unknown rack: " + rackId);
        rack.restock(qty);
    }

    public Optional<Rack> getRack(String rackId) {
        return Optional.ofNullable(racks.get(rackId));
    }

    public Map<String, Rack> getSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(racks));
    }
}
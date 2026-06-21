package model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import exception.InvalidProductException;

public class Inventory {
    private final ConcurrentHashMap<String, Rack> racks = new ConcurrentHashMap<>();

    public void addRack(Rack rack) {
        racks.put(rack.getCode(), rack);
    }

    public void removeRack(String code) {
        racks.remove(code);
    }

    public Rack getRack(String code) {
        Rack rack = racks.get(code);
        if (rack == null) throw new InvalidProductException(code);
        return rack;
    }

    public boolean isAvailable(String code) {
        Rack rack = racks.get(code);
        return rack != null && rack.isAvailable();
    }

    public Product getProduct(String code) {
        return getRack(code).getProduct();
    }

    public Map<String, Rack> getAllRacks() {
        return Collections.unmodifiableMap(racks);
    }
}

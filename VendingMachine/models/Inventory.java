package models;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Inventory {
    private final ConcurrentHashMap<String, Rack> racks;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    public Inventory() {
        this.racks = new ConcurrentHashMap<>();
    }

    public void addRack(Rack rack) {
        writeLock.lock();
        try {
            racks.put(rack.getCode(), rack);
        } finally {
            writeLock.unlock();
        }
    }

    public void removeRack(String code) {
        writeLock.lock();
        try {
            racks.remove(code);
        } finally {
            writeLock.unlock();
        }
    }

    public Rack getRack(String code) {
        readLock.lock();
        try {
            return racks.get(code);
        } finally {
            readLock.unlock();
        }
    }

    public boolean isAvailable(String code) {
        readLock.lock();
        try {
            Rack rack = racks.get(code);
            return rack != null && rack.isAvailable();
        } finally {
            readLock.unlock();
        }
    }

    public Product getProduct(String code) {
        readLock.lock();
        try {
            Rack rack = racks.get(code);
            if (rack == null) {
                throw new RuntimeException("Invalid product code: " + code);
            }
            return rack.getProduct();
        } finally {
            readLock.unlock();
        }
    }

    public void dispenseItem(String code) {
        writeLock.lock();
        try {
            Rack rack = racks.get(code);
            if (rack == null) {
                throw new RuntimeException("Invalid product code: " + code);
            }
            rack.dispense();
        } finally {
            writeLock.unlock();
        }
    }

    public void restock(String code, int quantity) {
        writeLock.lock();
        try {
            Rack rack = racks.get(code);
            if (rack == null) {
                throw new RuntimeException("Invalid product code: " + code);
            }
            rack.restock(quantity);
        } finally {
            writeLock.unlock();
        }
    }

    public Map<String, Integer> getReport() {
        readLock.lock();
        try {
            Map<String, Integer> report = new HashMap<>();
            for (Map.Entry<String, Rack> entry : racks.entrySet()) {
                report.put(entry.getValue().getProduct().getName() + " (" + entry.getKey() + ")",
                           entry.getValue().getQuantity());
            }
            return report;
        } finally {
            readLock.unlock();
        }
    }
}

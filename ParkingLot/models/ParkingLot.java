package models;

import java.util.ArrayList;
import java.util.List;

public class ParkingLot {
    private static ParkingLot instance;

    private String id;
    private String name;
    private Address address;
    private List<ParkingFloor> floors;
    private List<EntryGate> entryGates;
    private List<ExitGate> exitGates;

    private ParkingLot(String id, String name, Address address) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.floors = new ArrayList<>();
        this.entryGates = new ArrayList<>();
        this.exitGates = new ArrayList<>();
        System.out.println("Created Parking Lot: " + name);
    }

    public static synchronized ParkingLot getInstance(String id, String name, Address address) {
        if (instance == null) {
            instance = new ParkingLot(id, name, address);
        }
        return instance;
    }

    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    public void removeFloor(int floorNumber) {
        floors.removeIf(f -> f.getFloorNumber() == floorNumber);
    }

    public void addEntryGate(EntryGate gate) {
        entryGates.add(gate);
    }

    public void removeEntryGate(String gateId) {
        entryGates.removeIf(g -> g.getGateId().equals(gateId));
    }

    public void addExitGate(ExitGate gate) {
        exitGates.add(gate);
    }

    public void removeExitGate(String gateId) {
        exitGates.removeIf(g -> g.getGateId().equals(gateId));
    }

    public List<ParkingFloor> getFloors() {
        return floors;
    }

    public String getName() {
        return name;
    }
}

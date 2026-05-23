package models;

import enums.vechicle;
import models.parkingSpot.AbstractParkingSpot;

public class DisplayBoard {
    private String boardId;
    private ParkingFloor floor;

    public DisplayBoard(String boardId, ParkingFloor floor) {
        this.boardId = boardId;
        this.floor = floor;
    }

    public void showAvailability() {
        System.out.println("=== Display Board " + boardId + " | Floor " + floor.getFloorNumber() + " ===");
        for (vechicle type : vechicle.values()) {
            AbstractParkingSpot spot = floor.getNearestAvailableSpot(type);
            if (spot != null) {
                System.out.println(type + ": Available (nearest spot ID: " + spot.getSpotId() + ")");
            } else {
                System.out.println(type + ": FULL");
            }
        }
        System.out.println("====================================");
    }
}

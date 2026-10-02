package model;

public enum RoomStatus {
    AVAILABLE,
    BOOKED,           // reserved, guest not yet arrived
    OCCUPIED,         // guest checked in
    UNDER_MAINTENANCE;
}

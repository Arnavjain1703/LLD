package library.model;

public enum ReservationStatus {
    WAITING,    // in queue, waiting for a copy
    NOTIFIED,   // copy available, member has been told — window to collect is open
    COMPLETED,  // member collected the book
    CANCELLED,  // member cancelled before collection
    EXPIRED     // notified but did not collect within the expiry window
}

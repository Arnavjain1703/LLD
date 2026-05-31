# Hotel Management System — Architecture Details

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    HotelManagementSystem (Facade)                │
│                      [Singleton, Thread-Safe]                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │ SearchService│  │BookingManager│  │    PaymentService     │ │
│  │              │  │              │  │    (Strategy)         │ │
│  │ TreeMap-based│  │ AtomicLong   │  │                       │ │
│  │ range queries│  │ ScheduledExec│  │ DefaultPaymentService │ │
│  └──────┬───────┘  └──────┬───────┘  └───────────┬───────────┘ │
│         │                  │                      │             │
│  ┌──────┴──────────────────┴──────────────────────┴───────────┐ │
│  │                     RoomManager                            │ │
│  │         [ConcurrentHashMap + ReentrantLock per Room]        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              NotificationService (Observer)                 │ │
│  │         EmailNotificationService | SMSNotificationService  │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Responsibilities

### 1. HotelManagementSystem (Facade)
- **Pattern**: Singleton + Facade
- **Role**: Single entry point for all client operations
- **Thread Safety**: Double-checked locking for lazy initialization
- Delegates to specialized managers; does NOT hold business logic

### 2. RoomManager
- **Role**: Owns room inventory and reservation calendar
- **Key DS**: `ConcurrentHashMap<String, TreeMap<LocalDate, String>>` — per-room reservation timeline
- **Thread Safety**: `ReentrantLock` per room for allocation atomicity
- **Operations**: allocate, release, check availability, get available rooms

### 3. BookingManager
- **Role**: Manages booking lifecycle (create → confirm → check-in → check-out)
- **Key DS**: `ConcurrentHashMap<String, Booking>` for O(1) lookup
- **Thread Safety**: `AtomicLong` for ID generation; per-booking state transitions are synchronized
- **Hold Expiry**: `ScheduledExecutorService` schedules auto-cancellation after 15-min TTL

### 4. SearchService
- **Role**: Optimized room search across multiple dimensions
- **Key DS**:
  - `TreeSet<Room>` sorted by price → O(log n) price-range queries
  - `ConcurrentHashMap<RoomType, List<Room>>` → O(1) type filtering
- **Thread Safety**: Read-heavy; uses concurrent collections

### 5. PaymentService (Strategy Pattern)
- **Role**: Calculate totals, process payments, handle refunds
- **Interface**: Allows swapping payment implementations
- **Pricing**: `nights × basePrice × (1 + taxRate) + surcharges`

### 6. NotificationService (Observer Pattern)
- **Role**: Notify guests on booking events
- **Interface**: Pluggable (Email, SMS, Push)
- **Trigger Points**: booking confirmed, check-in, check-out, cancellation

---

## Concurrency Model

```
Thread-1 (Guest A books Room 101, May 1-5)
    │
    ├── RoomManager.allocateRoom("R101", May1, May5)
    │       │
    │       ├── room.acquireLock()  ←── ReentrantLock
    │       ├── check TreeMap for date conflicts
    │       ├── insert reservation entries
    │       ├── room.releaseLock()
    │       └── return true
    │
    └── BookingManager.createBooking(...)
            ├── AtomicLong.incrementAndGet() → bookingId
            ├── ConcurrentHashMap.put(bookingId, booking)
            └── scheduleHoldExpiry(bookingId, 15min)

Thread-2 (Guest B books Room 101, May 3-7) — CONCURRENT
    │
    ├── RoomManager.allocateRoom("R101", May3, May7)
    │       │
    │       ├── room.acquireLock()  ←── BLOCKS until Thread-1 releases
    │       ├── check TreeMap → CONFLICT DETECTED (May 3-5 overlap)
    │       ├── room.releaseLock()
    │       └── throw RoomNotAvailableException
```

---

## Date Overlap Detection Algorithm

```java
// Room's reservation timeline: TreeMap<LocalDate, String> (date → bookingId)
// A room is available for [checkIn, checkOut) if NO existing reservation
// overlaps with this range.

boolean isAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
    TreeMap<LocalDate, LocalDate> reservations = roomReservations.get(roomId);
    // Find the reservation that starts just before or at checkIn
    Map.Entry<LocalDate, LocalDate> lower = reservations.floorEntry(checkIn);
    // Find the reservation that starts just after checkIn
    Map.Entry<LocalDate, LocalDate> upper = reservations.ceilingEntry(checkIn);

    // Check if lower reservation's checkout overlaps with our checkIn
    if (lower != null && lower.getValue().isAfter(checkIn)) return false;
    // Check if upper reservation's checkIn is before our checkOut
    if (upper != null && upper.getKey().isBefore(checkOut)) return false;
    return true;
}
// Complexity: O(log n) using TreeMap floor/ceiling operations
```

---

## Hold Expiry Mechanism

```
┌─────────────┐     15 min TTL      ┌──────────────────┐
│   PENDING   │ ──────────────────── │  AUTO-CANCELLED  │
│  (booking)  │   ScheduledExec      │  room released   │
└─────────────┘                      └──────────────────┘
       │
       │ payment success
       ▼
┌─────────────┐
│  CONFIRMED  │
└─────────────┘
```

- `ScheduledExecutorService.schedule(() -> cancelIfPending(bookingId), 15, MINUTES)`
- On expiry: check if still PENDING → cancel booking → release room dates
- On payment success: update status to CONFIRMED → cancel scheduled task

---

## Package Structure

```
src/com/hotelmanagement/
├── enums/
│   ├── RoomType.java
│   ├── RoomStatus.java
│   ├── BookingStatus.java
│   ├── PaymentMethod.java
│   └── PaymentStatus.java
├── models/
│   ├── Hotel.java
│   ├── Room.java
│   ├── Guest.java
│   ├── Booking.java
│   └── Payment.java
├── service/
│   ├── RoomManager.java
│   ├── BookingManager.java
│   └── SearchService.java
├── payment/
│   ├── PaymentService.java
│   └── DefaultPaymentService.java
├── notification/
│   ├── NotificationService.java
│   └── EmailNotificationService.java
├── exceptions/
│   ├── RoomNotAvailableException.java
│   └── InvalidBookingException.java
├── HotelManagementSystem.java
└── Main.java
```

---

## Design Decisions & Trade-offs

| Decision | Rationale | Trade-off |
|---|---|---|
| ReentrantLock per Room (not global) | Maximizes parallelism — different rooms booked concurrently | More memory (one lock per room object) |
| TreeMap for reservations | O(log n) overlap detection vs O(n) linear scan | Slightly more complex than ArrayList |
| AtomicLong for IDs | Lock-free, faster than synchronized counter | IDs are sequential (predictable) |
| ScheduledExecutorService for TTL | Non-blocking hold expiry | Thread pool overhead |
| ConcurrentHashMap everywhere | Lock-free reads, segmented writes | Slightly more memory than HashMap |
| Singleton facade | Single coordination point | Harder to test (use dependency injection internally) |

---

## Complexity Analysis

| Operation | Time Complexity | Space Complexity |
|---|---|---|
| Search rooms by date range | O(R × log B) | O(1) |
| Book a room | O(log B) | O(1) |
| Cancel booking | O(log B) | O(1) |
| Check-in / Check-out | O(1) | O(1) |
| Search by room type | O(1) lookup + O(k) results | O(1) |
| Search by price range | O(log n + k) | O(k) |

Where: R = rooms in hotel, B = bookings per room, k = result set size, n = total rooms

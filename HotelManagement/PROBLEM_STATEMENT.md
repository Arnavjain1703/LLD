# Hotel Management System — Low Level Design (SDE2)

---

## Problem Statement

Design a **Hotel Management System** that handles room reservations, guest check-in/check-out,
and room inventory management. The system supports multiple hotels, each with various room types,
and must handle concurrent booking requests safely.

You are NOT expected to design the full stack — focus on the core domain model,
class relationships, concurrency handling, optimized data structures, and key method signatures.

---

## Functional Requirements

### FR1 — Hotel & Room Management
- Support multiple **hotels**, each identified by a unique ID, name, and location
- Each hotel has multiple **rooms** with types: `SINGLE`, `DOUBLE`, `DELUXE`, `SUITE`
- Rooms have a **floor number**, **room number**, **base price per night**, and **status**
- Room statuses: `AVAILABLE`, `RESERVED`, `OCCUPIED`, `UNDER_MAINTENANCE`
- Add/remove rooms from a hotel; update room pricing dynamically

### FR2 — Guest Management
- Register guests with: name, email, phone, ID proof
- Maintain guest booking history for loyalty/repeat-guest features
- Support guest search by name, email, or phone

### FR3 — Room Search & Availability
- Search available rooms by: **date range**, **room type**, **price range**, **hotel location**
- Given a hotel + date range → return all available rooms with pricing
- Availability must account for existing reservations (no double-booking)
- Optimized search using interval-based data structures for date-range queries

### FR4 — Booking & Reservation
- A guest selects a room for a specific **check-in** and **check-out** date
- The room is **temporarily held** (TTL = 15 minutes) while payment is processed
- Concurrent booking: if two guests attempt the same room for overlapping dates,
  only one succeeds — the other receives a `RoomNotAvailableException`
- Holds expire automatically; expired holds release the room back to `AVAILABLE`
- Booking states: `PENDING`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`

### FR5 — Check-in / Check-out
- On check-in: room status → `OCCUPIED`; booking status → `CHECKED_IN`
- On check-out: room status → `AVAILABLE`; booking status → `CHECKED_OUT`; final bill generated
- Early check-out and late check-out (with surcharge) supported

### FR6 — Payment Processing
- Calculate total: `(nights × base_price) + taxes + surcharges`
- Support multiple payment methods: `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `CASH`
- Payment states: `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`
- On cancellation before check-in: full refund; within 24hrs of check-in: 50% refund

### FR7 — Notifications
- On booking confirmation, check-in, check-out → notify guest (email/SMS)
- Model as a `NotificationService` interface — pluggable implementations

---

## Non-Functional Requirements

### NFR1 — Thread Safety & Concurrency
- Multiple guests can search and book rooms simultaneously
- Room allocation must be **atomic** — use `ReentrantLock` per room for fine-grained locking
- Booking ID generation must be thread-safe (AtomicLong)
- ConcurrentHashMap for all shared registries

### NFR2 — Optimized Data Structures
- **TreeMap<LocalDate, Set<Room>>** — for efficient date-range availability queries (O(log n) range lookups)
- **ConcurrentHashMap<RoomType, TreeSet<Room>>** — rooms sorted by price for type-based search
- **HashMap<String, Guest>** — O(1) guest lookup by email
- **PriorityQueue** — for room allocation by price preference
- **Interval-based overlap detection** — prevent double-booking using sorted date ranges

### NFR3 — Design Patterns Used
- **Singleton** — HotelManagementSystem facade
- **Strategy** — Payment processing, pricing calculation
- **Observer** — Notification on booking events
- **Builder** — Complex Booking object construction
- **Factory** — Room creation based on type

### NFR4 — Scalability Considerations
- System should handle 10K+ concurrent searches
- Booking throughput: 1000+ bookings/minute
- Lock granularity: per-room (not global) to maximize parallelism

---

## Constraints & Assumptions

1. Check-in time: 2:00 PM; Check-out time: 11:00 AM (configurable)
2. Maximum booking duration: 30 nights
3. A room can only have ONE active reservation for any given date
4. All monetary values in INR (double precision)
5. In-memory storage only — no database persistence required
6. Single JVM deployment — no distributed locking needed

---

## API Signatures (Key Operations)

```java
// Search
List<Room> searchAvailableRooms(String hotelId, LocalDate checkIn, LocalDate checkOut, RoomType type);

// Booking
Booking createBooking(String guestId, String roomId, LocalDate checkIn, LocalDate checkOut);
void cancelBooking(String bookingId);

// Check-in / Check-out
void checkIn(String bookingId);
void checkOut(String bookingId);

// Payment
Payment processPayment(String bookingId, PaymentMethod method);
Payment processRefund(String bookingId);
```

---

## Edge Cases to Handle

1. **Double-booking attempt** — two guests book same room for overlapping dates concurrently
2. **Hold expiry** — payment not completed within 15 minutes
3. **Cancellation after check-in** — should be rejected
4. **Search during concurrent booking** — room should not appear available once held
5. **Check-out before check-in date** — invalid operation
6. **Overlapping date ranges** — `[May 1-5]` and `[May 3-7]` conflict; `[May 1-5]` and `[May 5-7]` do NOT (check-out day = available for new check-in)

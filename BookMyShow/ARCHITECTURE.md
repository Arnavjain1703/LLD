# BookMyShow — Architecture & Design Decisions

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Design Patterns Used](#2-design-patterns-used)
3. [Data Structures & Justification](#3-data-structures--justification)
4. [Thread Safety & Concurrency](#4-thread-safety--concurrency)
5. [Key Design Decisions](#5-key-design-decisions)
6. [SOLID Principles Applied](#6-solid-principles-applied)
7. [Booking Flow (End-to-End)](#7-booking-flow-end-to-end)
8. [How to Run](#8-how-to-run)

---

## 1. Project Structure

```
src/com/bookmyshow/
├── Main.java                              # E2E demo driver
├── enums/
│   ├── BookingStatus.java                 # PENDING | CONFIRMED | CANCELLED
│   ├── Genre.java                         # ACTION | COMEDY | DRAMA | THRILLER | HORROR
│   ├── SeatStatus.java                    # AVAILABLE | LOCKED | BOOKED
│   └── SeatType.java                      # REGULAR(1.0) | PREMIUM(1.5) | RECLINER(2.0)
├── models/
│   ├── Booking.java                       # Booking entity (volatile + synchronized)
│   ├── Movie.java                         # Immutable movie metadata
│   ├── Screen.java                        # 2D Seat[][] layout + shows list
│   ├── Seat.java                          # Immutable seat (row/col/type/price)
│   ├── SeatLock.java                      # TTL-based temporary lock (immutable)
│   ├── Show.java                          # ConcurrentHashMap<seatId, SeatStatus>
│   └── Theater.java                       # Aggregates screens
├── service/
│   └── BookingService.java                # ★ Singleton + Facade + Inline Data + Concurrency
├── strategy/
│   ├── PricingStrategy.java               # Strategy interface
│   ├── DefaultPricingStrategy.java        # basePrice × seatType multiplier
│   ├── SurgePricingStrategy.java          # basePrice × seatType × surge multiplier
│   ├── CancellationPolicy.java            # Strategy interface
│   └── StandardCancellationPolicy.java    # 2-hour cutoff before show
├── observer/
│   ├── NotificationService.java           # Observer interface
│   ├── EmailNotificationService.java      # Email channel
│   └── SMSNotificationService.java        # SMS channel
├── payment/
│   ├── PaymentService.java                # Payment interface
│   └── MockPaymentService.java            # Always succeeds (demo)
└── exceptions/
    ├── SeatNotAvailableException.java     # Seat is LOCKED or BOOKED
    └── ShowOverlapException.java          # Schedule conflict
```

**Total: 15 Java files** — focused on demonstrating design patterns, concurrency, and clean architecture without boilerplate layers.

---

## 2. Design Patterns Used

### 2.1 Singleton Pattern — `BookingService`

**Why:** A single coordinating instance must own the `showLocks` map. Multiple instances would each have their own lock maps, defeating mutual exclusion for seat booking.

**Implementation:** `volatile` + Double-Checked Locking (DCL)

```java
private static volatile BookingService INSTANCE;

public static BookingService getInstance(PricingStrategy pricing,
                                         CancellationPolicy cancellation,
                                         PaymentService payment,
                                         List<NotificationService> observers) {
    if (INSTANCE == null) {                          // 1st check (no lock)
        synchronized (BookingService.class) {
            if (INSTANCE == null)                    // 2nd check (under lock)
                INSTANCE = new BookingService(pricing, cancellation, payment, observers);
        }
    }
    return INSTANCE;
}
```

**Why volatile + DCL instead of simpler alternatives?**
- Eager init: wasteful if never used
- `synchronized` on every call: unnecessary contention after creation
- `volatile` + DCL: lock-free on the fast path (after first creation), safe publication guaranteed

---

### 2.2 Strategy Pattern — `PricingStrategy` and `CancellationPolicy`

**Why:** Business rules for pricing and cancellation change independently from the booking flow. Encapsulating them behind interfaces lets us swap implementations at runtime without modifying `BookingService`.

| Interface | Implementations | Logic |
|-----------|----------------|-------|
| `PricingStrategy` | `DefaultPricingStrategy`, `SurgePricingStrategy` | `basePrice × seatType.multiplier × [surgeMultiplier]` |
| `CancellationPolicy` | `StandardCancellationPolicy` | Allow if current time < showStart − 2 hours |

**Default Pricing:**
```java
seats.stream()
    .mapToDouble(s -> s.getBasePrice() * s.getType().getMultiplier())
    .sum();
```

**Surge Pricing (adds configurable multiplier):**
```java
seats.stream()
    .mapToDouble(s -> s.getBasePrice() * s.getType().getMultiplier() * surgeMultiplier)
    .sum();
```

**Extensibility:** Adding a `WeekendPricingStrategy` or `NoCancellationPolicy` requires one new class — zero changes to existing code.

---

### 2.3 Observer Pattern — `NotificationService`

**Why:** Booking confirmation/cancellation triggers multiple side effects (email, SMS). The core service should not know about each notification channel.

**Implementation:**
- `NotificationService` interface: `onBookingConfirmed(Booking)` / `onBookingCancelled(Booking)`
- `BookingService` holds `List<NotificationService>` injected via constructor
- After every state transition, all observers are notified:
  ```java
  observers.forEach(o -> o.onBookingConfirmed(booking));
  ```

**Adding a new channel (push, WhatsApp):** Create one class implementing `NotificationService`, pass it in the list. Zero changes to `BookingService`.

---

### 2.4 Facade Pattern — `BookingService`

**Why:** The booking lifecycle spans: lock seats → calculate price → process payment → mark booked → create booking → notify. `BookingService` orchestrates this entire flow behind three clean methods:

| Method | Responsibility |
|--------|---------------|
| `lockSeats(showId, seatIds, userId)` | Validate availability → atomically lock → create TTL-based `SeatLock` |
| `confirmBooking(lockId)` | Verify lock → calculate price → charge payment → mark BOOKED → create Booking → notify |
| `cancelBooking(bookingId, userId)` | Verify ownership → check policy → release seats → refund → notify |

Consumers never interact with payment, notification, or concurrency primitives directly.

---

## 3. Data Structures & Justification

### 3.1 Core Data Structures

| Data Structure | Location | Why This Choice |
|---------------|----------|-----------------|
| **`ConcurrentHashMap<String, SeatStatus>`** | `Show.seatStatusMap` | O(1) per-seat lookup. Thread-safe per-key writes without global locking. Multiple threads can read different seats simultaneously. The primary concurrency workhorse for seat availability tracking. |
| **`ConcurrentHashMap<String, ReentrantLock>`** | `BookingService.showLocks` | One lock per show — bookings on different shows never contend. `computeIfAbsent()` creates locks lazily and atomically. Dramatically better throughput than a single global lock. |
| **`ConcurrentHashMap<String, Show>`** | `BookingService.shows` | O(1) show lookup by ID. Thread-safe CRUD without external synchronization. Inline data store replacing a separate repository layer. |
| **`ConcurrentHashMap<String, Booking>`** | `BookingService.bookings` | O(1) booking retrieval. Same thread-safety guarantees. Allows concurrent booking lookups across different users. |
| **`ConcurrentHashMap<String, SeatLock>`** | `BookingService.locks` | O(1) lock lookup by lockId. Supports lazy expiry sweep — `removeIf()` on values for expired locks. |
| **`Seat[][]` (2D array)** | `Screen.seatLayout` | Fixed-size, allocated once at construction. Row/col addressing maps naturally to a physical theater. More memory-efficient than `List<List<Seat>>` (no per-element wrapper objects). O(1) positional access. |
| **`ArrayList<Show>`** | `Screen.shows` | Ordered list of shows scheduled on a screen. Small size (few shows per screen per day). `synchronized` access + `Collections.unmodifiableList()` for external reads. |
| **`ArrayList<Screen>`** | `Theater.screens` | Same pattern — small, ordered, synchronized. Theaters have a fixed small number of screens. |
| **`List.copyOf()`** | `SeatLock.seatIds`, `Booking.seats` | Immutable snapshot at construction. Prevents external mutation after object creation. Thread-safe by nature — no synchronization needed for reads. |

### 3.2 Why Not Alternatives?

| Alternative | Rejected Because |
|------------|------------------|
| `HashMap` for seat status | Not thread-safe. Would need `synchronized` on every access, serializing all reads. |
| `TreeMap` for seats | O(log n) lookups. We don't need sorted order — O(1) by ID is sufficient. |
| `List<Seat>` for screen layout | Loses row/col spatial relationship. 2D array gives natural `[row][col]` addressing matching physical theater layout. |
| `synchronized HashMap` | `ConcurrentHashMap` offers better throughput — concurrent reads are lock-free, writes use segment-level locking. |
| `AtomicReference<SeatStatus>` per seat | Overkill for single-seat atomics. We need **multi-seat** atomic transitions (lock 3 seats together), which requires an explicit `ReentrantLock` across the group. |
| `ReadWriteLock` for locks store | Lazy expiry via `ConcurrentHashMap.values().removeIf()` is simpler and adequate for the access pattern. |
| `LinkedHashMap` for shows | Insertion order doesn't matter for overlap checking — we iterate all shows anyway. `ArrayList` is simpler. |

### 3.3 ID Generation Strategy

**`UUID` (truncated)** — Used for Booking IDs (`BKG-`), Payment IDs (`PAY-`), Lock IDs (`LCK-`).

**Why UUID over sequential counter:**
- No central coordination needed (works in distributed systems)
- No contention on an `AtomicLong` counter
- Prefix (`BKG-`, `PAY-`, `LCK-`) gives instant type identification in logs

---

## 4. Thread Safety & Concurrency

### 4.1 Core Problem: Double Booking

Two users selecting the same seat simultaneously must result in exactly one success. Solved with **per-show `ReentrantLock`**:

```java
ReentrantLock lock = showLocks.computeIfAbsent(showId, k -> new ReentrantLock());
lock.lock();
try {
    // Atomic check-then-set: ALL seats must be AVAILABLE
    for (String seatId : seatIds) {
        SeatStatus status = show.getSeatStatus(seatId);
        if (status != SeatStatus.AVAILABLE)
            throw new SeatNotAvailableException("Seat " + seatId + " is " + status);
    }
    // Only if all pass → mark all LOCKED atomically
    seatIds.forEach(id -> show.updateSeatStatus(id, SeatStatus.LOCKED));
} finally {
    lock.unlock();  // ALWAYS release — even on exception
}
```

**Why per-show locks instead of a global lock?**
- Users booking Show A and Show B never contend
- Only threads targeting the **same show** serialize
- Real-world: thousands of shows, only a handful of concurrent users per show

---

### 4.2 Concurrency Mechanisms Summary

| Mechanism | Where | Purpose |
|-----------|-------|---------|
| `ConcurrentHashMap` | `Show.seatStatusMap`, all BookingService stores | Lock-free concurrent reads, segment-level writes |
| `ReentrantLock` per show | `BookingService.showLocks` | Multi-seat atomic check-then-set (prevents double booking) |
| `volatile` | `Booking.status`, `Booking.paymentId` | Cross-thread visibility without `synchronized` for reads |
| `synchronized` methods | `Booking.confirm()`, `Booking.cancel()` | Atomic state transitions on a single booking |
| `synchronized` methods | `Screen.addShow()`, `Theater.addScreen()` | Safe list mutation during setup |
| `volatile` + DCL | `BookingService.INSTANCE` | Thread-safe lazy singleton initialization |
| `List.copyOf()` | Immutable collections in SeatLock, Booking | Thread-safe by immutability — no locks needed |
| `Collections.unmodifiableList()` | `Screen.getShows()`, `Theater.getScreens()` | Prevents external callers from mutating internal lists |

---

### 4.3 Volatile Fields in Booking

```java
private volatile BookingStatus status;   // visible across threads immediately
private volatile String paymentId;       // set during confirm, read from any thread
```

**Why volatile here:** If Thread A confirms a booking (sets `status = CONFIRMED`), Thread B must see `CONFIRMED` immediately — without entering a `synchronized` block for every status read. `volatile` provides this visibility guarantee.

**Why additionally `synchronized` on `confirm()`/`cancel()`:** Ensures that the compound operation (set status + set paymentId) happens atomically — no thread sees `CONFIRMED` with a `null` paymentId.

---

### 4.4 Lock Expiry — Lazy Sweep (No Background Threads)

Before every `lockSeats()` and `getSeatMap()` call, stale locks are expired:

```java
private void expireStaleLocksFor(String showId) {
    Show show = shows.get(showId);
    if (show == null) return;
    locks.values().removeIf(lock -> {
        if (lock.getShowId().equals(showId) && lock.isExpired()) {
            lock.getSeatIds().forEach(id -> show.updateSeatStatus(id, SeatStatus.AVAILABLE));
            return true;  // remove from map
        }
        return false;
    });
}
```

**Why lazy sweep over `ScheduledExecutorService`:**
- Simpler — no thread lifecycle management, no shutdown hooks
- No timing edge cases with thread pool
- Adequate for in-memory design (production would use Redis TTL or DB row expiry)
- Expiry only matters when someone actually tries to book — lazy is sufficient

---

### 4.5 try-finally on Every Lock Acquisition

```java
lock.lock();
try { /* critical section */ }
finally { lock.unlock(); }
```

Prevents deadlocks if any exception (RuntimeException, OOM, SeatNotAvailableException) occurs inside the critical section. The lock is **always** released.

---

### 4.6 Immutability as a Concurrency Tool

| Class | Approach |
|-------|----------|
| `Seat` | All fields `final` — shared across threads without synchronization |
| `Movie` | All fields `final` — read-only metadata |
| `SeatLock` | All fields `final`, `List.copyOf()` for seatIds — immutable after construction |
| `Booking.seats` | `List.copyOf()` — immutable snapshot of reserved seats |

Immutable objects are inherently thread-safe — zero synchronization cost for sharing.

---

## 5. Key Design Decisions

### 5.1 Inline Data Stores (No Repository Layer)

**Decision:** `BookingService` holds `ConcurrentHashMap` stores directly instead of separate repository classes.

**Why:** For an interview/LLD context, the repository layer adds indirection without demonstrating new concepts. The thread safety, O(1) lookups, and data management are all visible in one place.

**Trade-off:** In production, you'd extract `IBookingRepository`, `IShowRepository` behind interfaces for:
- Database swapability
- Unit testing with mocks
- Separation of concerns

---

### 5.2 Seat Layout as 2D Array

Row 0 = RECLINER, Row 1 = PREMIUM, Rows 2+ = REGULAR.

Seat IDs encode position: `{screenId}-R{row}C{col}` (e.g., `T1-SC1-R0C3`).

**Benefits:**
- O(1) identification from ID string
- Natural mapping to physical theater layout
- `resolveSeats()` iterates the fixed layout to find seats by ID

---

### 5.3 Two-Phase Booking (Lock → Confirm)

**Phase 1 — `lockSeats()`:** Temporarily reserves seats (TTL = 10 minutes). User can review selection, enter payment details.

**Phase 2 — `confirmBooking()`:** Processes payment, permanently marks seats as BOOKED.

**Why two phases:**
- Prevents indefinite holds on seats
- Allows payment processing time without blocking other users
- If user abandons, lock expires automatically via lazy sweep

---

### 5.4 Show Overlap Validation (in Main)

30-minute buffer between shows for cleanup/next audience entry:

```java
newShow.start < existing.end + 30min AND newShow.end > existing.start → OVERLAP
```

This logic lives in `Main.hasOverlap()` for the demo. In production, it would be in a `ShowService`.

---

### 5.5 Cancellation Guards

Before processing any cancellation, two checks:
1. **Ownership:** `booking.getUserId().equals(userId)` — can't cancel someone else's booking
2. **Policy:** `cancellationPolicy.canCancel(booking)` — time-window check (2hr cutoff)

---

## 6. SOLID Principles Applied

| Principle | Application |
|-----------|-------------|
| **S**ingle Responsibility | `BookingService` handles booking lifecycle. `PricingStrategy` handles pricing. `NotificationService` handles notifications. Each has one reason to change. |
| **O**pen/Closed | New pricing? Implement `PricingStrategy`. New cancellation rule? Implement `CancellationPolicy`. New notification channel? Implement `NotificationService`. No existing code changes. |
| **L**iskov Substitution | `SurgePricingStrategy` replaces `DefaultPricingStrategy` seamlessly — same interface contract, same return type semantics. |
| **I**nterface Segregation | `PaymentService` only exposes `processPayment()` + `refund()`. `NotificationService` only exposes `onBookingConfirmed()` + `onBookingCancelled()`. No fat interfaces. |
| **D**ependency Inversion | `BookingService` depends on `PricingStrategy` (abstraction), not `DefaultPricingStrategy` (concrete). All dependencies injected via constructor. |

---

## 7. Booking Flow (End-to-End)

```
User                    BookingService              Show              Payment        Observers
 │                           │                       │                  │               │
 │── lockSeats(showId, ──────>│                       │                  │               │
 │    seatIds, userId)        │                       │                  │               │
 │                            │── expireStaleLocksFor()                  │               │
 │                            │── acquireReentrantLock(showId)           │               │
 │                            │── checkAllSeatsAvailable() ──>│         │               │
 │                            │<── allAvailable ──────────────│         │               │
 │                            │── markAllLOCKED() ───────────>│         │               │
 │                            │── releaseLock()               │         │               │
 │                            │── createSeatLock(TTL=10min)   │         │               │
 │<── SeatLock ───────────────│                               │         │               │
 │                            │                               │         │               │
 │── confirmBooking(lockId) ──>│                               │         │               │
 │                            │── verifyLockNotExpired()       │         │               │
 │                            │── resolveSeats()              │         │               │
 │                            │── pricingStrategy.calculatePrice()      │               │
 │                            │── paymentService.processPayment() ──────>│               │
 │                            │<── paymentId ───────────────────────────│               │
 │                            │── markAllBOOKED() ───────────>│         │               │
 │                            │── createBooking()             │         │               │
 │                            │── booking.confirm(paymentId)  │         │               │
 │                            │── observers.forEach(notify) ──────────────────────────>│
 │<── Booking ────────────────│                               │         │               │
```

---

## 8. How to Run

```bash
# From the BookMyShow directory
cd src
javac com/bookmyshow/Main.java com/bookmyshow/**/*.java
java com.bookmyshow.Main
```

**The demo exercises:**
- Theater + Screen + Seat layout construction
- Show scheduling with overlap detection
- Two users booking different seats (Alice: RECLINER, Bob: PREMIUM)
- 5-thread concurrency race for the same seat (only 1 wins)
- Cancellation with policy check + ownership verification + seat release
- Surge pricing demonstration (1.5x multiplier)
- Email + SMS notifications on confirm/cancel

---

## Summary Table

| Concern | Approach |
|---------|----------|
| Double-booking prevention | Per-show `ReentrantLock` + atomic check-then-set |
| Lock expiry | Lazy sweep on `lockSeats()`/`getSeatMap()` calls |
| Pricing flexibility | Strategy pattern — injected at construction |
| Cancellation rules | Strategy pattern — swappable policy |
| Notifications | Observer pattern — multiple decoupled channels |
| Data storage | Inline `ConcurrentHashMap` stores (no repository layer) |
| Singleton safety | `volatile` + double-checked locking |
| Immutability | `final` fields, `List.copyOf()`, defensive copies |
| Seat layout | 2D array — natural row/col theater mapping |
| ID generation | UUID with type prefix (`BKG-`, `PAY-`, `LCK-`) |

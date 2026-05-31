# BookMyShow — Low Level Design (SDE2)

---

## Problem Statement

Design a **Movie Ticket Booking System** similar to BookMyShow.
The system allows users to search for movies, select shows, pick seats, and confirm bookings.

You are NOT expected to design the full stack — focus on the core domain model,
class relationships, concurrency handling, and key method signatures.

---

## Functional Requirements

### FR1 — Theater & Show Management
- Add/remove theaters; each theater has multiple **screens**
- Each screen has a fixed **seat layout** (rows × columns)
- Seats have types: `REGULAR`, `PREMIUM`, `RECLINER`
- Add **movies** with metadata: title, duration (mins), language, genre
- Schedule **shows** — a movie on a specific screen at a specific date/time
- A screen cannot have two **overlapping shows**
  (overlap check must account for movie duration + a 30-min cleanup buffer)

### FR2 — Seat Browsing
- Given a city → list all movies currently showing
- Given a movie + city → list all shows with available seat count
- Given a show → return full seat map with per-seat status: `AVAILABLE`, `LOCKED`, `BOOKED`

### FR3 — Seat Selection & Locking
- A user selects one or more seats for a show
- Selected seats are **temporarily locked** (TTL = 10 minutes) while the user pays
- Concurrent seat selection: if two users attempt the same seat simultaneously,
  only one succeeds — the other receives a `SeatNotAvailableException`
- Locks expire automatically; expired locks return seats to `AVAILABLE`

### FR4 — Booking & Payment
- Confirm a booking by completing payment for locked seats
- On **success**: seats → `BOOKED`; a `Booking` record is created with a unique ID
- On **failure or TTL expiry**: seats → `AVAILABLE`; lock is discarded
- A booking can be **cancelled** up to 2 hours before show start time

### FR5 — Notifications
- On booking confirmation, notify the user (email / SMS)
- Model as a `NotificationService` interface — no implementation required

---

## Non-Functional Requirements

| Concern        | Requirement                                                      |
|----------------|------------------------------------------------------------------|
| Concurrency    | Seat locking must be thread-safe; no double-booking              |
| Consistency    | Seat status transitions must be atomic                           |
| Extensibility  | Easy to add new seat types, pricing rules, cancellation policies |
| Scope          | In-memory design; no DB or network layer needed                  |
| Payment        | Model as a `PaymentService` interface; no gateway internals      |

---

## Entities to Design

```
Movie
  - id: String
  - title: String
  - durationMins: int
  - language: String
  - genre: Genre (enum)

Theater
  - id: String
  - name: String
  - city: String
  - screens: List<Screen>

Screen
  - id: String
  - name: String
  - seats: Seat[][]          ← 2D layout
  - shows: List<Show>

Seat
  - id: String
  - row: int
  - col: int
  - type: SeatType           ← REGULAR | PREMIUM | RECLINER
  - basePrice: double

Show
  - id: String
  - movie: Movie
  - screen: Screen
  - startTime: LocalDateTime
  - endTime: LocalDateTime   ← derived: startTime + movie.durationMins
  - seatStatusMap: Map<String, SeatStatus>   ← seatId → status

SeatLock
  - seatId: String
  - showId: String
  - userId: String
  - lockedAt: LocalDateTime
  - expiresAt: LocalDateTime

Booking
  - id: String
  - userId: String
  - show: Show
  - seats: List<Seat>
  - status: BookingStatus    ← PENDING | CONFIRMED | CANCELLED
  - totalPrice: double
  - createdAt: LocalDateTime
```

---

## Enums

```java
enum SeatType    { REGULAR, PREMIUM, RECLINER }
enum SeatStatus  { AVAILABLE, LOCKED, BOOKED }
enum BookingStatus { PENDING, CONFIRMED, CANCELLED }
enum Genre       { ACTION, COMEDY, DRAMA, THRILLER, HORROR }
```

---

## Core Service Methods (Expected Signatures)

```java
// MovieService
List<Movie> searchMoviesByCity(String city);
List<Show>  getShowsForMovie(String movieId, String city);

// ShowService
Map<String, SeatStatus> getSeatMap(String showId);
boolean addShow(Show show);   // must validate no overlap on screen

// BookingService
SeatLock    lockSeats(String showId, List<String> seatIds, String userId)
                throws SeatNotAvailableException;

Booking     confirmBooking(String lockId, PaymentDetails payment)
                throws PaymentFailedException;

boolean     cancelBooking(String bookingId)
                throws CancellationNotAllowedException;
```

---

## Key Design Challenges

### 1. Concurrency — Preventing Double Booking
Two users select seat A3 for the same show simultaneously.
How do you ensure only one succeeds?

**Approach:**
- Maintain a `ConcurrentHashMap<String, Object> showLocks` (showId → lock object)
- Inside `lockSeats()`, `synchronized(showLocks.get(showId))` before mutating seatStatusMap
- Check status → lock → update atomically within the synchronized block

### 2. Lock Expiry
How do you return expired locked seats to AVAILABLE?

**Approach A (lazy):** On every `getSeatMap()` or `lockSeats()` call, sweep the lock map
and expire stale locks before responding. No background thread needed.

**Approach B (eager):** A `ScheduledExecutorService` runs every 1 minute to purge expired locks.

### 3. Seat Pricing
PREMIUM = 1.5× base, RECLINER = 2× base. Where does this logic live?

**Approach:** `PricingStrategy` interface with a `calculatePrice(Seat seat)` method.
Default implementation reads multiplier from `SeatType` enum.
Allows future strategies (surge pricing, discount codes) without touching core classes.

### 4. Show Overlap Validation
Before adding a new show to a screen, validate no time overlap.

```java
// In ShowService.addShow():
for (Show existing : screen.getShows()) {
    if (newShow.getStartTime().isBefore(existing.getEndTime().plusMinutes(30))
     && newShow.getEndTime().isAfter(existing.getStartTime())) {
        throw new ShowOverlapException();
    }
}
```

### 5. Cancellation Policy
"Cancel up to 2 hours before show" — where does this rule live?

**Approach:** `CancellationPolicy` interface with `canCancel(Booking booking): boolean`.
Default: `show.startTime.minusHours(2).isAfter(LocalDateTime.now())`.
Inject into `BookingService` — swap policy without changing booking logic.

---

## Design Patterns to Call Out

| Pattern   | Where Applied                                              |
|-----------|------------------------------------------------------------|
| Strategy  | `PricingStrategy`, `CancellationPolicy`                    |
| Observer  | `NotificationService` triggered on booking state change    |
| Singleton | `BookingService`, `TheaterRepository` (single instance)    |
| Factory   | `BookingFactory.create(show, seats, user)` builds Booking  |
| Repository| `TheaterRepository`, `ShowRepository` — data access layer  |

---

## Interview Time Plan

| Phase                          | Time   |
|--------------------------------|--------|
| Clarify requirements           | 5 min  |
| Entities + enums + class diagram | 15 min |
| Core method implementations    | 20 min |
| Concurrency handling           | 10 min |
| Design pattern callouts        | 5 min  |
| **Total**                      | **55 min** |

---

## Out of Scope

- Actual payment gateway
- User authentication / authorization
- Database schema
- REST API design
- Admin UI for theater management

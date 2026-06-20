# Thread Safety & Data Structure Choices

## Thread Safety Mechanisms Used

### 1. `ReentrantReadWriteLock` on ParkingFloor

Instead of plain `synchronized` (which blocks ALL threads even if they're just reading), we use a ReadWriteLock that allows **multiple concurrent readers OR one exclusive writer**.

```java
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

// READ — multiple display boards can check availability simultaneously
public AbstractParkingSpot getNearestAvailableSpot(vechicle vehicleType) {
    readLock.lock();
    try {
        for (AbstractParkingSpot spot : availableSpots) {
            if (spot.canFitVehicle(vehicleType)) return spot;
        }
        return null;
    } finally {
        readLock.unlock();
    }
}

// WRITE — only one thread can find + claim a spot at a time
public AbstractParkingSpot findAndParkNearest(vechicle vehicleType) {
    writeLock.lock();
    try {
        for (AbstractParkingSpot spot : availableSpots) {
            if (spot.canFitVehicle(vehicleType)) {
                availableSpots.remove(spot);  // find + remove atomically
                return spot;
            }
        }
        return null;
    } finally {
        writeLock.unlock();
    }
}
```

**Why ReadWriteLock over `synchronized`:**

| Scenario | `synchronized` | `ReadWriteLock` |
|---|---|---|
| 5 DisplayBoards checking availability | Only 1 at a time (all block) | All 5 run concurrently |
| 2 cars entering same floor | 1 at a time | 1 at a time (write is exclusive) |
| 1 car entering + 3 boards reading | All block each other | Writer waits for readers to finish, then runs alone |

**Rules of ReadWriteLock:**
- Multiple threads can hold the **read lock** simultaneously
- Only ONE thread can hold the **write lock**, and no readers allowed during it
- Write lock waits for all current readers to finish before acquiring
- `finally { unlock() }` ensures the lock is always released, even on exceptions

**Why `Reentrant`:**
- Same thread can acquire the same lock multiple times without deadlocking itself
- Useful if a write method calls another write method internally

---

### 2. `AtomicInteger` for Ticket and Payment counters

```java
private static final AtomicInteger counter = new AtomicInteger(0);
this.ticketId = "TKT-" + counter.incrementAndGet();
```

**Why:** Multiple gates generate tickets concurrently. `AtomicInteger` uses CPU-level CAS (Compare-And-Swap) — a single hardware instruction that atomically reads, compares, and writes without needing a lock.

**How CAS works internally:**
```
while (true) {
    int current = value;                    // 1. read
    int next = current + 1;                 // 2. compute
    if (CPU.compareAndSwap(current, next))  // 3. atomic CPU instruction
        return next;                        //    success!
    // another thread changed it → retry
}
```

**CAS vs synchronized:**
| `synchronized` | `AtomicInteger` (CAS) |
|---|---|
| Thread blocks (sleeps) waiting | Thread spins (retries immediately) |
| OS involvement to park/wake threads | Pure CPU instruction, no OS call |
| Good for multi-step operations | Best for single-variable updates |

---

### 3. `volatile` + Double-Checked Locking for Singleton

```java
private static volatile ParkingLot instance;

public static ParkingLot getInstance(...) {
    if (instance == null) {                  // fast path — no lock
        synchronized (ParkingLot.class) {
            if (instance == null) {          // double-check inside lock
                instance = new ParkingLot(...);
            }
        }
    }
    return instance;
}
```

**Why:**
- `volatile` prevents a thread from seeing a partially-constructed object (stops CPU instruction reordering).
- First `if` check avoids locking on every `getInstance()` call after initialization.
- Second `if` check (inside lock) prevents double creation if two threads pass the first check simultaneously.

---

### 4. `CopyOnWriteArrayList` for floors/gates lists

```java
this.floors = new CopyOnWriteArrayList<>();
this.entryGates = new CopyOnWriteArrayList<>();
this.exitGates = new CopyOnWriteArrayList<>();
```

**Why:** These lists are read frequently (every vehicle entry iterates floors) but written rarely (adding/removing a floor or gate is an admin operation). CopyOnWriteArrayList creates a new internal array on write, so readers never block and never see inconsistent state.

**How it works:**
- On `add()`/`remove()` → copies the entire internal array, modifies the copy, then swaps the reference
- On iteration → reads the current array snapshot (never sees concurrent writes)
- No locking needed for readers

---

### 5. `synchronized` on `park()` / `unpark()` in AbstractParkingSpot

```java
public synchronized void park(Vehical vehical) {
    if (this.state == parkingSpotState.OCCUPIED)
        throw new IllegalStateException("Spot already occupied");
    setState(parkingSpotState.OCCUPIED);
    this.vehical = vehical;
}

public synchronized void unpark() {
    if (this.state == parkingSpotState.AVAILABLE)
        throw new IllegalStateException("Spot already empty");
    setState(parkingSpotState.AVAILABLE);
    this.vehical = null;
}
```

**Why:** Guards the spot's state transition (AVAILABLE ↔ OCCUPIED) and vehicle reference. The check-then-act must be atomic — without `synchronized`, two threads could both see AVAILABLE and both try to park.

---

### 6. Defensive copy in `getSpots()` (under read lock)

```java
public List<AbstractParkingSpot> getSpots() {
    readLock.lock();
    try {
        return new ArrayList<>(spots);
    } finally {
        readLock.unlock();
    }
}
```

**Why:** Prevents external code from iterating the internal list while another thread modifies it. Returns a snapshot that won't change under the caller. Uses read lock since it's just copying — multiple threads can call this concurrently.

---

### 7. `CountDownLatch` for coordination in Main

```java
CountDownLatch entryLatch = new CountDownLatch(numVehicles);

// Each thread:
entryLatch.countDown();  // "I'm done"

// Main thread:
entryLatch.await();      // waits until all threads are done
```

**Why:** We need all vehicles to finish entering before we display availability or start exits. CountDownLatch lets the main thread wait for N concurrent operations to complete.

---

### 8. `ExecutorService` thread pool

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> { ... });
```

**Why:** Reuses a fixed number of threads instead of creating/destroying threads per vehicle. Simulates real-world scenario where multiple gates operate simultaneously.

---

### 9. Ticket stores floor reference

```java
public Ticket(Vehical vehicle, AbstractParkingSpot spot, ParkingFloor floor) {
    this.floor = floor;  // remember which floor the spot belongs to
}
```

**Why:** When the vehicle exits, we need to return the spot to the correct floor's `availableSpots` TreeSet. Without this, the spot would be marked AVAILABLE but never added back to the pool — permanently lost.

---

## Data Structures Used

| Data Structure | Where Used | Why This DS | Time Complexity |
|---|---|---|---|
| **TreeSet** | `ParkingFloor.availableSpots` | Keeps spots sorted by `spotId`. Nearest = `first()`, Farthest = `last()`. Auto-removes/adds maintain order. | O(log n) insert/remove/find |
| **ReentrantReadWriteLock** | `ParkingFloor` | Allows multiple readers (DisplayBoards) concurrently while writers (entry/exit) are exclusive. Better throughput than `synchronized` when reads >> writes. | O(1) lock/unlock |
| **CopyOnWriteArrayList** | `ParkingLot.floors`, `entryGates`, `exitGates` | Lock-free reads for high-read/low-write scenario. No `ConcurrentModificationException`. | O(1) read, O(n) write |
| **AtomicInteger** | `Ticket.counter`, `Payment.counter` | Lock-free thread-safe counter using hardware CAS. Zero contention cost when uncontested. | O(1) amortized |
| **ArrayList** | `ParkingFloor.spots` (all spots including occupied) | Simple indexed collection for total spot tracking. Protected by read/write lock. | O(1) add, O(n) remove |
| **CountDownLatch** | `Main` (coordination) | One-shot barrier — main thread waits until N threads signal completion. | O(1) countDown/await |
| **ExecutorService (FixedThreadPool)** | `Main` (thread management) | Reuses fixed set of threads instead of creating new threads per task. Limits concurrency to N. | — |

---

## Why TreeSet for Available Spots

The key optimization. Without TreeSet:
```
Find nearest spot: scan all spots → O(n)
Find farthest spot: scan all spots → O(n)
```

With TreeSet (sorted by spotId):
```
Find nearest spot: iterate from first() → O(log n) to O(n) worst case
Find farthest spot: iterate from last() via descendingSet() → O(log n) to O(n) worst case
Add spot back: TreeSet.add() → O(log n)
Remove spot: TreeSet.remove() → O(log n)
```

TreeSet also ensures:
- Only AVAILABLE spots are in the set (occupied spots are removed)
- Natural ordering by `spotId` via `Comparable<AbstractParkingSpot>`
- No duplicates

---

## How the Critical Path Works (Entry Gate)

```
Thread A (Gate 1)                    Thread B (Gate 2)               Thread C (DisplayBoard)
        |                                    |                              |
        v                                    v                              v
floor.findAndParkNearest(CAR)      floor.findAndParkNearest(CAR)   floor.getNearestAvailableSpot(CAR)
        |                                    |                              |
   [acquires WRITE lock]                     |  (BLOCKED — write waits)     |  (BLOCKED — read waits for writer)
   iterates TreeSet                          |                              |
   finds spot 1000                           |                              |
   removes spot 1000 from TreeSet            |                              |
   [releases WRITE lock]                     |                              |
        |                               [acquires WRITE lock]            |  (still waiting)
        v                               iterates TreeSet                |
   spot.park(vehicle)                   spot 1000 is GONE               |
   ticket = new Ticket(...)             finds spot 1001 instead         |
        |                               removes spot 1001               |
        v                               [releases WRITE lock]           |
   DONE — vehicle 1001                       |                     [acquires READ lock]
   parked at spot 1000                       v                     reads TreeSet (no spot 1000/1001)
                                        spot.park(vehicle)         returns spot 1002
                                        ticket = new Ticket(...)   [releases READ lock]
                                             |                          |
                                             v                          v
                                        DONE — vehicle 1002        DisplayBoard shows availability
                                        parked at spot 1001
```

**Key insight:** Thread C (DisplayBoard) doesn't block Thread A and Thread B from each other.
But if two DisplayBoards (Thread C and Thread D) both wanted to read simultaneously, they CAN — 
multiple READ locks are held at the same time. Only WRITE is exclusive.

No two threads ever get the same spot.

---

## How Exit Returns Spot to Pool

```
Thread C (Exit Gate)
        |
        v
ticket.getSpot().unpark()           → synchronized on spot: sets state to AVAILABLE
        |
        v
ticket.getFloor().unparkVehicle()   → acquires WRITE lock on floor
                                    → adds spot back to TreeSet
                                    → releases WRITE lock
        |
        v
   spot is now findable by entry gates again
```

---

## Where Multithreading is Implemented

### `Main.java` — Concurrent Entry/Exit Simulation

```java
ExecutorService executor = Executors.newFixedThreadPool(4);  // 4 threads = 4 gate operators
```

**Phase 1: 6 vehicles enter concurrently**
```java
for (int i = 0; i < numVehicles; i++) {
    executor.submit(() -> {
        tickets[idx] = gate.processEntry(vehicles[idx], lot.getFloors());
    });
}
entryLatch.await();  // main thread waits for all 6 to finish
```
- 6 vehicles submitted to 4 threads → threads race to grab spots
- Two different gates used (NearestSpotStrategy and FarthestSpotStrategy)
- `CountDownLatch(6)` ensures we wait for all entries before proceeding

**Phase 2: 3 vehicles exit concurrently**
```java
for (int i = 0; i < numExits; i++) {
    executor.submit(() -> {
        gate.processExit(tickets[idx], PaymentMethod.CASH, new CashPaymentProcessor());
    });
}
exitLatch.await();  // main thread waits for all 3 to finish
```
- 3 vehicles exit simultaneously through 2 different exit gates
- Spots are freed and returned to the available pool concurrently

**Phase 3: New vehicle enters freed spot**
- Proves that spots freed in Phase 2 are correctly available for reuse

---

### `ParkingFloor.java` — ReentrantReadWriteLock Critical Section

All spot-modifying methods acquire the **write lock**, read-only methods acquire the **read lock**:
```java
// WRITE LOCK — exclusive (entry/exit operations)
public AbstractParkingSpot findAndParkNearest(vechicle vehicleType)
public AbstractParkingSpot findAndParkFarthest(vechicle vehicleType)
public void unparkVehicle(AbstractParkingSpot spot)
public void addSpot(AbstractParkingSpot spot)
public void removeSpot(AbstractParkingSpot spot)

// READ LOCK — shared (display boards, getters)
public AbstractParkingSpot getNearestAvailableSpot(vechicle vehicleType)
public List<AbstractParkingSpot> getSpots()
```

Multiple DisplayBoards can read concurrently, but only one entry/exit thread modifies the TreeSet at a time.

---

### `AbstractParkingSpot.java` — Synchronized State Transitions

```java
public synchronized void park(Vehical vehical)   // AVAILABLE → OCCUPIED
public synchronized void unpark()                 // OCCUPIED → AVAILABLE
```

Prevents two threads from parking in the same spot or unparking simultaneously.

---

### `Ticket.java` / `Payment.java` — AtomicInteger Counters

```java
private static final AtomicInteger counter = new AtomicInteger(0);
this.ticketId = "TKT-" + counter.incrementAndGet();
```

Multiple threads create tickets/payments simultaneously — `AtomicInteger` guarantees unique IDs without locks.

---

### `ParkingLot.java` — Thread-Safe Singleton + Lists

```java
private static volatile ParkingLot instance;               // singleton
private List<ParkingFloor> floors = new CopyOnWriteArrayList<>();  // concurrent reads
```

- Singleton is safe for concurrent first-access via double-checked locking
- Floor/gate lists are safe for concurrent iteration (entry gates iterate floors while admin adds/removes)

---

### Flow of Multithreading Through the System

```
Main.java (ExecutorService with 4 threads)
    │
    ├── Thread-1: entryGate1.processEntry(car1, floors)
    ├── Thread-2: entryGate2.processEntry(car2, floors)
    ├── Thread-3: entryGate1.processEntry(car3, floors)
    ├── Thread-4: entryGate2.processEntry(car4, floors)
    │         │
    │         ▼
    │   EntryGate.processEntry()
    │         │
    │         ▼
    │   NearestSpotStrategy.findParkingSpot()
    │         │
    │         ▼
    │   ParkingFloor.findAndParkNearest()  ← WRITE LOCK (only 1 thread at a time per floor)
    │         │
    │         ▼
    │   AbstractParkingSpot.park()          ← SYNCHRONIZED (only 1 thread at a time per spot)
    │         │
    │         ▼
    │   new Ticket() → AtomicInteger        ← LOCK-FREE (CAS hardware instruction)
    │
    ▼
CountDownLatch.await()  ← main thread waits here until all entries done
    │
    ▼
Exit phase (same pattern with unparkVehicle)
```

---

## Summary Table

| Concern | Solution | DS/Mechanism |
|---------|----------|---|
| Two threads get same spot | Atomic `findAndPark*()` under write lock | `ReentrantReadWriteLock` + TreeSet |
| Multiple readers blocked unnecessarily | Read lock allows concurrent reads | `ReentrantReadWriteLock.ReadLock` |
| Duplicate ticket/payment IDs | Lock-free counter | `AtomicInteger` (CAS) |
| Partially constructed singleton | Prevent reordering | `volatile` + DCL |
| Concurrent list read/write | Copy-on-write snapshot | `CopyOnWriteArrayList` |
| Spot state corruption | Lock on individual spot | `synchronized` on park/unpark |
| Leaking mutable internal state | Return snapshot under read lock | Defensive copy (`new ArrayList<>`) |
| Wait for N threads to finish | Count-based barrier | `CountDownLatch` |
| Efficient thread reuse | Fixed pool | `ExecutorService` |
| Return spot on exit | Ticket stores floor ref | Floor reference in Ticket |

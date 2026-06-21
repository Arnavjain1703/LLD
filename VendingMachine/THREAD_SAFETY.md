# Thread Safety & Data Structure Choices

## Thread Safety Model — Layered, No Redundancy

Each layer owns exactly one concern. No double-locking.

```
┌───────────────────────────────────────────────────────────┐
│  VendingMachine (synchronized on user-facing methods)     │
│  ── serializes state machine transitions                  │
│                                                           │
│  ┌─────────────────────────────┐  ┌────────────────────┐ │
│  │  Inventory (ConcurrentHM)   │  │ ButtonPanel (CHM)  │ │
│  │  ── admin add/remove safe   │  │ ── dynamic buttons │ │
│  │  ── single-key lookups      │  └────────────────────┘ │
│  │                             │                          │
│  │  ┌───────────────────────┐  │                          │
│  │  │  Rack (AtomicInteger) │  │                          │
│  │  │  ── CAS dispense      │  │                          │
│  │  └───────────────────────┘  │                          │
│  └─────────────────────────────┘                          │
└───────────────────────────────────────────────────────────┘
```

---

## 1. `synchronized` on VendingMachine Public Methods

```java
public synchronized void selectProduct(String code) {
    currentState.selectProduct(this, code);
}

public synchronized void insertMoney(double amount) {
    currentState.insertMoney(this, amount);
}

public synchronized void dispense() {
    currentState.dispense(this);
}

public synchronized void cancelTransaction() {
    currentState.cancel(this);
}
```

**Why:** The state pattern delegates to the current state object. A state transition involves reading state, acting, and possibly changing state — this check-then-act sequence must be atomic. Without `synchronized`, two threads could both see `HasMoneyState`, both attempt to dispense, and double-dispense a single item.

**Trade-off:** A vending machine typically serves one user at a time, so `synchronized` on public methods is the simplest correct approach. If we needed higher throughput (e.g., multiple vending machines behind one API), we'd use finer-grained locking or a lock-per-machine map.

---

## 2. `volatile` + Double-Checked Locking for Singleton

```java
private static volatile VendingMachine instance;

public static VendingMachine getInstance(String machineId) {
    if (instance == null) {
        synchronized (VendingMachine.class) {
            if (instance == null) {
                instance = new VendingMachine(machineId);
            }
        }
    }
    return instance;
}
```

**Why:**
- `volatile` prevents a thread from seeing a partially-constructed object (stops CPU instruction reordering via memory barrier).
- First `if` check avoids locking on every `getInstance()` call after initialization (fast path).
- Second `if` check (inside lock) prevents double creation if two threads pass the first check simultaneously.

---

## 3. `ConcurrentHashMap` on Inventory

```java
private final ConcurrentHashMap<String, Rack> racks = new ConcurrentHashMap<>();

public void addRack(Rack rack) {
    racks.put(rack.getCode(), rack);
}

public boolean isAvailable(String code) {
    Rack rack = racks.get(code);
    return rack != null && rack.isAvailable();
}
```

**Why ConcurrentHashMap (not RWLock):**

All Inventory operations are **single-key** lookups or mutations:
- `get(code)` — single key read
- `put(code, rack)` — single key write
- `remove(code)` — single key write

There are no compound operations that span multiple keys (e.g., "check key A then write key B"). ConcurrentHashMap handles all of these atomically without external locking.

**Why the old design (RWLock + HashMap) was over-engineered:**

| Concern | RWLock approach | ConcurrentHashMap approach |
|---|---|---|
| Admin read (report) | ReadLock | Just iterate (weakly consistent) |
| Admin write (add/remove) | WriteLock | `put`/`remove` (atomic) |
| User reads (isAvailable) | ReadLock | `get` (atomic) |
| Complexity | High — lock/unlock in try/finally everywhere | Zero — just call methods |
| Risk of deadlock | Possible if lock ordering violated | None — no explicit locks |

`ReadWriteLock` is useful when you have **compound multi-key operations** that must be atomic. We don't have those here.

---

## 4. `AtomicInteger` with CAS Loop on Rack

```java
private final AtomicInteger quantity;

public void dispense() {
    int current = quantity.get();
    while (current > 0) {
        if (quantity.compareAndSet(current, current - 1)) {
            return;
        }
        current = quantity.get();
    }
    throw new OutOfStockException(code);
}

public void restock(int amount) {
    int current = quantity.get();
    while (true) {
        int newVal = current + amount;
        if (newVal > maxCapacity) {
            throw new IllegalArgumentException("Exceeds max capacity");
        }
        if (quantity.compareAndSet(current, newVal)) {
            return;
        }
        current = quantity.get();
    }
}
```

**Why AtomicInteger over `synchronized`:**

| Aspect | `synchronized` | `AtomicInteger` CAS |
|---|---|---|
| What it guards | Entire method body | Single int field |
| Blocking | Yes — thread waits | No — thread retries (spin) |
| Throughput under contention | Low — serial access | High — no context switch |
| Correctness | check-then-act in one block | check-then-CAS (atomic) |
| Complexity | Lower | Slightly higher (loop) |

The CAS loop is optimal here because:
1. We only guard a single integer (quantity).
2. Contention is low (dispense is already serialized by machine lock for users).
3. Admin restock can proceed without blocking user dispense.
4. No risk of deadlock — no locks held.

**How CAS works:**
```
Thread A: read quantity=5, try CAS(5→4)  → succeeds, quantity=4
Thread B: read quantity=5, try CAS(5→4)  → FAILS (expected 5, found 4)
Thread B: retry: read quantity=4, try CAS(4→3) → succeeds, quantity=3
```

---

## 5. `ConcurrentHashMap` on ButtonPanel

```java
private final ConcurrentHashMap<String, Button> productButtons = new ConcurrentHashMap<>();

public void addProductButton(String code) {
    productButtons.put(code, new Button("SELECT " + code, () -> machine.selectProduct(code)));
}

public void pressProductButton(String code) {
    Button button = productButtons.get(code);
    if (button == null) { ... }
    button.press();
}
```

**Why:** Admin can add/remove product buttons while a user is interacting with the panel. CHM allows safe concurrent read (user pressing) and write (admin adding).

---

## Where Multithreading Is Relevant

### Scenario: Admin Restocks While User Purchases

```
Thread A (User)                          Thread B (Admin)
        |                                       |
        v                                       v
machine.selectProduct("A1")              admin.restock("A1", 5)
   [acquires machine monitor]                   |
   state.selectProduct(...)                     |
   inventory.isAvailable("A1")                  |  (NOT blocked — CHM.get is non-blocking)
      → rack.isAvailable() → true              |
   machine.setSelectedProduct(...)              |
   machine.setState(HasMoney)                   |
   [releases machine monitor]                   |
        |                                       v
        v                                  inventory.getRack("A1")  ← CHM.get, non-blocking
machine.insertMoney(2.00)                  rack.restock(5)          ← AtomicInteger CAS
   [acquires machine monitor]                  quantity: 5 → 10 ← CAS succeeds
   state.insertMoney(...)                      |
   balance += 2.00                             v
   [releases machine monitor]              DONE — restock complete
        |
        v
machine.dispense()
   [acquires machine monitor]
   state.dispense(...)
   → charge payment
   → rack.dispense()  ← AtomicInteger CAS: 10 → 9
   → return change
   → resetTransaction()
   [releases machine monitor]
        |
        v
   DONE — item dispensed
```

**Key insight:** The machine monitor serializes the user's transaction (select → insert → dispense is atomic). But admin operations (restock, add product) never need the machine monitor — they operate directly on Inventory (CHM) and Rack (AtomicInteger). Zero contention between admin and user paths.

---

### Scenario: 10 Threads Competing for Same Product

```
Thread-0 through Thread-9 all call: machine.selectProduct("C1"), insertMoney(1.00), dispense()

Since all are synchronized on machine:
  Thread-0: acquires monitor → select → insert → dispense → release  (success)
  Thread-1: acquires monitor → select → insert → dispense → release  (success)
  Thread-2: acquires monitor → select → insert → dispense → release  (success)
  ...
  Thread-7: acquires monitor → select → "C1 out of stock" → release  (fails gracefully)
  Thread-8: acquires monitor → select → "C1 out of stock" → release  (fails gracefully)
  Thread-9: acquires monitor → select → "C1 out of stock" → release  (fails gracefully)
```

No double-dispense. No race condition. The `synchronized` ensures complete isolation of each transaction.

---

## Data Structures Summary

| Data Structure | Where Used | Why This DS | Time Complexity |
|---|---|---|---|
| **ConcurrentHashMap** | `Inventory.racks`, `ButtonPanel.productButtons` | Lock-free single-key ops; concurrent read/write without explicit locks | O(1) amortized get/put |
| **AtomicInteger** | `Rack.quantity` | CAS-based atomic decrement; no blocking, no deadlock risk | O(1) per CAS attempt |
| **synchronized** | `VendingMachine` user methods | Serializes state machine transitions (simplest correct approach) | O(1) acquire/release |
| **volatile** | `VendingMachine.instance` | Memory barrier for DCL singleton — prevents partial construction visibility | — |
| **CountDownLatch** | `Main` (test coordination) | One-shot barrier — main waits for N threads to complete | O(1) countDown/await |
| **ExecutorService** | `Main` (thread pool) | Reuses fixed thread set; avoids thread creation overhead | — |

---

## Why NOT These Alternatives

| Alternative | Why Rejected |
|---|---|
| **RWLock on Inventory** | All operations are single-key — CHM handles this atomically. RWLock adds try/finally boilerplate with no benefit. |
| **`synchronized` on Rack** | Only one int field to guard. AtomicInteger CAS is lock-free and non-blocking — better under admin-restock-during-user-dispense. |
| **`synchronized` on Inventory** | Would block all readers during any write. CHM allows concurrent reads and writes to different keys. |
| **StampedLock** | More performant than RWLock for read-heavy, but we eliminated the need for any explicit lock on Inventory entirely. |
| **Lock-free state machine (AtomicReference)** | Transactions are multi-step (select → insert → dispense). CAS on state alone doesn't protect balance/selectedProduct mutations. Machine-level synchronized is simpler and correct. |

---

## Critical Path Summary

```
User Action          →  Lock Layer           →  Data Layer
────────────────────────────────────────────────────────────────────
selectProduct("A1")  →  machine monitor      →  CHM.get("A1"), AtomicInt.get()
insertMoney(2.00)    →  machine monitor      →  (just balance += 2.00)
dispense()           →  machine monitor      →  AtomicInt.CAS(n → n-1)
restock("A1", 5)     →  (no machine lock!)   →  CHM.get("A1"), AtomicInt.CAS(n → n+5)
addProduct("B3")     →  (no machine lock!)   →  CHM.put("B3", rack), CHM.put("B3", button)
```

Admin operations are **completely non-blocking** with respect to user operations. They never compete for the machine monitor.

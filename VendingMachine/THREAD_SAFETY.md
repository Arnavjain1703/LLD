# Thread Safety & Data Structure Choices

## Thread Safety Mechanisms Used

### 1. `synchronized` on VendingMachine Public Methods

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

**Trade-off:** A vending machine typically serves one user at a time, so `synchronized` on public methods is the simplest correct approach. If we needed higher throughput (e.g., multiple vending machines behind one API), we'd use finer-grained locking.

---

### 2. `volatile` + Double-Checked Locking for Singleton

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
- `volatile` prevents a thread from seeing a partially-constructed object (stops CPU instruction reordering).
- First `if` check avoids locking on every `getInstance()` call after initialization.
- Second `if` check (inside lock) prevents double creation if two threads pass the first check simultaneously.

---

### 3. `ReentrantReadWriteLock` on Inventory

```java
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

// READ — multiple threads can read inventory concurrently
public boolean isAvailable(String code) {
    readLock.lock();
    try {
        Rack rack = racks.get(code);
        return rack != null && rack.isAvailable();
    } finally {
        readLock.unlock();
    }
}

// WRITE — only one thread can modify inventory at a time
public void dispenseItem(String code) {
    writeLock.lock();
    try {
        Rack rack = racks.get(code);
        rack.dispense();
    } finally {
        writeLock.unlock();
    }
}
```

**Why ReadWriteLock over `synchronized`:**

| Scenario | `synchronized` | `ReadWriteLock` |
|---|---|---|
| 3 admins checking inventory report | Only 1 at a time | All 3 run concurrently |
| Admin restocking + user dispensing | 1 at a time | 1 at a time (write is exclusive) |
| User checking availability + admin reading report | Both block | Both read concurrently |

**Rules of ReadWriteLock:**
- Multiple threads can hold the **read lock** simultaneously
- Only ONE thread can hold the **write lock**, and no readers allowed during it
- Write lock waits for all current readers to finish before acquiring
- `finally { unlock() }` ensures the lock is always released, even on exceptions

---

### 4. `synchronized` on Rack Methods

```java
public synchronized void dispense() {
    if (quantity <= 0) {
        throw new RuntimeException("Rack " + code + " is empty");
    }
    quantity--;
}

public synchronized void restock(int amount) {
    if (quantity + amount > maxCapacity) {
        throw new RuntimeException("Restock exceeds max capacity");
    }
    quantity += amount;
}
```

**Why:** Guards the rack's quantity field. The check-then-decrement must be atomic — without `synchronized`, admin could restock while dispense is mid-check, or two threads could both see quantity=1 and both try to decrement.

---

### 5. `ConcurrentHashMap` for Rack Storage

```java
private final ConcurrentHashMap<String, Rack> racks;
```

**Why:** Even though we wrap access with ReadWriteLock for multi-step operations, using ConcurrentHashMap provides a safety net — individual get/put operations are inherently thread-safe. This prevents any edge cases where a thread accesses the map without holding the lock (defensive programming).

---

## Where Multithreading is Relevant

### Scenario: Admin Restocks While User Purchases

```
Thread A (User)                          Thread B (Admin)
        |                                       |
        v                                       v
machine.selectProduct("A1")              admin.restock("A1", 5)
   [acquires machine lock]                      |  (BLOCKED on inventory write lock
   state.selectProduct(...)                     |   if user is mid-dispense)
   checks inventory.isAvailable("A1")           |
      [acquires inventory read lock]            |
      rack.isAvailable() → true                 |
      [releases inventory read lock]            |
   [releases machine lock]                      |
        |                                       |
        v                                       v
machine.insertMoney(2.00)                inventory.restock("A1", 5)
   [acquires machine lock]                 [acquires inventory write lock]
   state.insertMoney(...)                  rack.restock(5)
   [releases machine lock]                   [acquires rack lock]
        |                                    quantity += 5
        v                                    [releases rack lock]
machine.dispense()                         [releases inventory write lock]
   [acquires machine lock]                      |
   state.dispense(...)                          v
   inventory.dispenseItem("A1")            DONE — restock complete
      [acquires inventory write lock]
      rack.dispense()
        [acquires rack lock]
        quantity--
        [releases rack lock]
      [releases inventory write lock]
   [releases machine lock]
        |
        v
   DONE — item dispensed
```

**Key insight:** The machine lock serializes user operations (select → insert → dispense is always atomic from the user's perspective). The inventory ReadWriteLock allows admin reads (reports) to happen concurrently with each other but serializes admin writes (restock, add/remove) against user dispensing.

---

## Data Structures Used

| Data Structure | Where Used | Why This DS | Time Complexity |
|---|---|---|---|
| **ConcurrentHashMap** | `Inventory.racks` | Thread-safe map with high concurrency for independent keys. Defensive layer under ReadWriteLock. | O(1) get/put |
| **ReentrantReadWriteLock** | `Inventory` | Allows multiple concurrent readers (availability checks, reports) while writers (dispense, restock) are exclusive. | O(1) lock/unlock |
| **synchronized** | `VendingMachine` methods, `Rack` methods | Serializes state transitions and quantity mutations. Simple and correct for single-user machine. | O(1) |
| **volatile** | `VendingMachine.instance` | Prevents partially-constructed singleton from being visible to other threads. | — |
| **CountDownLatch** | `Main` (coordination) | One-shot barrier — main thread waits until N threads signal completion. | O(1) countDown/await |
| **ExecutorService** | `Main` (thread management) | Reuses fixed set of threads for concurrent simulation. | — |

---

## How the Critical Path Works (User Purchase)

```
User Action          →  Machine Method (synchronized)  →  State Method  →  Inventory (RWLock)  →  Rack (synchronized)
─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
selectProduct("A1")  →  machine.selectProduct()         →  IdleState     →  readLock: isAvailable  →  rack.isAvailable()
insertMoney(2.00)    →  machine.insertMoney()           →  IdleState     →  (no inventory access)  →  —
dispense()           →  machine.dispense()              →  HasMoneyState →  (transitions to DispensingState)
                         machine.dispense()             →  DispensingState → writeLock: dispenseItem → rack.dispense()
```

The entire purchase is atomic from external perspective because `machine.selectProduct()`, `machine.insertMoney()`, and `machine.dispense()` are all `synchronized` on the same machine instance.

---

## Summary Table

| Concern | Solution | DS/Mechanism |
|---------|----------|---|
| Two threads triggering state transition simultaneously | Serialize all user operations | `synchronized` on VendingMachine methods |
| Admin restock during user dispense | Exclusive write access to inventory | `ReentrantReadWriteLock` write lock |
| Multiple admins reading inventory concurrently | Shared read access | `ReentrantReadWriteLock` read lock |
| Race on rack quantity (check-then-decrement) | Atomic quantity mutation | `synchronized` on Rack methods |
| Partially constructed singleton | Memory visibility guarantee | `volatile` + Double-Checked Locking |
| Defensive thread-safe map access | Concurrent map | `ConcurrentHashMap` |
| Coordination in demo | Wait for N threads | `CountDownLatch` |
| Thread reuse | Fixed pool | `ExecutorService` |

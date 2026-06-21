# Vending Machine — Low-Level Design (E2E)

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        VENDING MACHINE                           │
│                                                                 │
│  ┌──────────────┐    ┌──────────────────┐    ┌──────────────┐  │
│  │  ButtonPanel  │───>│  VendingMachine   │<───│ AdminService  │  │
│  │              │    │   (Singleton)     │    │              │  │
│  │ ┌──────────┐│    │                  │    │ - addProduct  │  │
│  │ │  Button  ││    │  ┌────────────┐  │    │ - restock     │  │
│  │ │ (Command)││    │  │   State    │  │    │ - remove      │  │
│  │ └──────────┘│    │  │  Machine   │  │    └──────────────┘  │
│  │ - Product   │    │  └────────────┘  │                      │
│  │ - Dispense  │    │                  │                      │
│  │ - Cancel    │    │  ┌────────────┐  │    ┌──────────────┐  │
│  │ - Display   │    │  │ Inventory  │  │    │  Payment     │  │
│  └──────────────┘    │  │(CHM+Atomic)│  │───>│  Processor   │  │
│                      │  └────────────┘  │    │ (Strategy)   │  │
│  ┌──────────────┐    │                  │    └──────────────┘  │
│  │   Display    │<───│  ┌────────────┐  │                      │
│  │  (Output)    │    │  │  Dispense  │  │    ┌──────────────┐  │
│  └──────────────┘    │  │  Strategy  │  │    │  Exception   │  │
│                      │  └────────────┘  │    │  Hierarchy   │  │
│                      └──────────────────┘    └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. State Machine

### States
| State | Description |
|-------|-------------|
| `IdleState` | Machine is waiting for user interaction |
| `HasMoneyState` | Product selected, accepting money / ready to dispense |
| `DispensingState` | Item is being dispensed |

### Transition Diagram

```
                    ┌─────────────────────────────────────┐
                    │                                     │
                    v                                     │
            ┌──────────────┐                             │
            │              │  selectProduct(valid)        │
     ┌─────>│  IDLE STATE  │──────────────────────┐      │
     │      │              │                      │      │
     │      └──────────────┘                      v      │
     │              │                     ┌──────────────┐│
     │              │                     │              ││
     │     cancel   │                     │  HAS_MONEY   ││
     │   (no-op)    │                     │    STATE     ││
     │              │                     │              ││
     │              │                     └──────┬───────┘│
     │              │                            │        │
     │              │              ┌─────────────┼────────┘
     │              │              │             │
     │              │         cancel()     dispense()
     │              │         (refund)    (funds >= price)
     │              │              │             │
     │              │              │             v
     │              │              │     ┌──────────────┐
     │              │              │     │              │
     │              │              │     │  DISPENSING   │
     │              │              │     │    STATE     │
     │              │              │     │              │
     │              │              │     └──────┬───────┘
     │              │              │            │
     └──────────────┴──────────────┘────────────┘
                 (all paths return to IDLE)
```

### Allowed Actions Per State

| Action | IdleState | HasMoneyState | DispensingState |
|--------|-----------|---------------|-----------------|
| `selectProduct()` | Validate stock → move to HasMoney | Throw `IllegalStateTransitionException` | Throw `IllegalStateTransitionException` |
| `insertMoney()` | Throw `IllegalStateTransitionException` | Add to balance, show balance | Throw `IllegalStateTransitionException` |
| `dispense()` | Throw `IllegalStateTransitionException` | Check funds → dispense or throw | Execute dispense → Idle |
| `cancel()` | No-op message | Refund balance → Idle | Throw `IllegalStateTransitionException` |

---

## 3. Component Details

### 3.1 ButtonPanel (User Input Interface)

Physical panel with `Button` objects. Each button encapsulates an action (Command pattern via `Runnable`).

| Component | Type | Action |
|-----------|------|--------|
| Product Buttons (A1, B1...) | `ConcurrentHashMap<String, Button>` | `machine.selectProduct(code)` |
| Dispense Button | `Button` | `machine.dispense()` |
| Cancel Button | `Button` | `machine.cancelTransaction()` |
| Display Button | `Button` | `machine.displayProducts()` |
| Coin Slot | Physical slot (not a button) | `machine.insertMoney(amount)` |

Product buttons are dynamically registered via `addProductButton(code)` when admin adds products.

### 3.2 Display (Output Interface)

All user-facing output goes through `Display`. Stateless — renders what it's told:

| Method | When Shown |
|--------|-----------|
| `showWelcome()` | After reset to idle |
| `showProducts(inventory)` | User presses display button |
| `showMessage(msg)` | General info (selection confirmed, sufficient funds) |
| `showInsertMoney(balance, required)` | After product selection / money inserted |
| `showDispenseSuccess(productName)` | Item dispensed successfully |
| `showChange(amount)` | Overpaid, returning change |
| `showRefund(amount)` | Transaction cancelled |
| `showError(error)` | Invalid action or failure |

### 3.3 Payment Processor (Strategy Pattern)

```
PaymentProcessor
├── CashPaymentProcessor   (physical coins/notes)
└── CardPaymentProcessor   (card tap/swipe)
```

Both implement:
- `charge(amount)` — charge the user
- `refund(amount)` — return money (change or cancellation)

Machine can swap processor at runtime via `setPaymentProcessor()`.

### 3.4 Dispense Strategy (Strategy Pattern)

```
DispenseStrategy
└── StandardDispenseStrategy   (decrement rack via AtomicInteger, release item)
```

Extensible for future strategies (e.g., fragile item handling, temperature-controlled).

### 3.5 Exception Hierarchy

```
VendingMachineException (RuntimeException)
├── InvalidProductException        — unknown product code
├── OutOfStockException            — rack is empty
├── InsufficientFundsException     — balance < price
├── PaymentFailedException         — processor returned false
└── IllegalStateTransitionException — action invalid in current state
```

States throw typed exceptions for invalid operations. The caller (`Main` or controller) catches `VendingMachineException` at the boundary.

---

## 4. E2E User Flows (Sequence Diagrams)

### Flow 1: Happy Path — Successful Purchase

```
User            ButtonPanel       VendingMachine     IdleState        HasMoneyState     DispensingState    Inventory    Display     PaymentProcessor
 │                  │                  │                │                  │                  │              │            │              │
 │─press Display───>│                  │                │                  │                  │              │            │              │
 │                  │──displayProducts()─>              │                  │                  │              │            │              │
 │                  │                  │────────────────────────────────────────────────────────────────────>showProducts()              │
 │                  │                  │                │                  │                  │              │            │              │
 │─press "A1"─────>│                  │                │                  │                  │              │            │              │
 │                  │──selectProduct("A1")──────────────>                  │                  │              │            │              │
 │                  │                  │                │─isAvailable("A1")────────────────────────────────>│            │              │
 │                  │                  │                │<──true───────────────────────────────────────────│            │              │
 │                  │                  │                │─setSelectedProduct()                  │              │            │              │
 │                  │                  │                │─────────────────────────────────────────────────────────────>showMessage()     │
 │                  │                  │                │─────────────────────────────────────────────────────────────>showInsertMoney() │
 │                  │                  │                │─setState(HasMoney)                    │              │            │              │
 │                  │                  │                │                  │                  │              │            │              │
 │─insert $1.00───>│                  │                │                  │                  │              │            │              │
 │                  │──insertMoney(1.00)────────────────────────────────>  │                  │              │            │              │
 │                  │                  │                │                  │─addBalance(1.00)  │              │            │              │
 │                  │                  │                │                  │─────────────────────────────────────────────>showInsertMoney()
 │                  │                  │                │                  │                  │              │            │              │
 │─insert $1.00───>│                  │                │                  │                  │              │            │              │
 │                  │──insertMoney(1.00)────────────────────────────────>  │                  │              │            │              │
 │                  │                  │                │                  │─addBalance(1.00)  │              │            │              │
 │                  │                  │                │                  │─────────────────────────────────────────────>showMessage("Sufficient funds")
 │                  │                  │                │                  │                  │              │            │              │
 │─press Dispense─>│                  │                │                  │                  │              │            │              │
 │                  │──dispense()──────────────────────────────────────>  │                  │              │            │              │
 │                  │                  │                │                  │─balance>=price?  │              │            │              │
 │                  │                  │                │                  │──────────────────────────────────────────────────────────>charge(1.50)
 │                  │                  │                │                  │<─────────────────────────────────────────────────────────true
 │                  │                  │                │                  │─setState(Dispensing)───────────>│              │            │
 │                  │                  │                │                  │                  │              │            │              │
 │                  │                  │                │                  │                  │─rack.dispense()─>          │              │
 │                  │                  │                │                  │                  │──────────────────────────────────────────>refund($0.50)
 │                  │                  │                │                  │                  │──────────────────────────>showChange()   │
 │                  │                  │                │                  │                  │──────────────────────────>showDispenseSuccess()
 │                  │                  │                │                  │                  │─resetTransaction()         │              │
 │<─────────────────────────── Item + $0.50 change ────────────────────────────────────────────────────────────────────────────────────│
```

### Flow 2: Cancellation — User Cancels Mid-Transaction

```
User            ButtonPanel       VendingMachine       IdleState       HasMoneyState     Display     PaymentProcessor
 │                  │                  │                  │                │               │              │
 │─press "B2"─────>│                  │                  │                │               │              │
 │                  │──selectProduct("B2")──────────────>│                │               │              │
 │                  │                  │                  │─validate stock │               │              │
 │                  │                  │                  │─setState(HasMoney)──>          │              │
 │                  │                  │                  │               │──────────────>showMessage()   │
 │                  │                  │                  │               │──────────────>showInsertMoney()
 │                  │                  │                  │               │               │              │
 │─insert $1.00───>│                  │                  │               │               │              │
 │                  │──insertMoney(1.00)─────────────────────────────────>               │              │
 │                  │                  │                  │               │─addBalance()  │              │
 │                  │                  │                  │               │──────────────>showInsertMoney()
 │                  │                  │                  │               │               │              │
 │─press Cancel───>│                  │                  │               │               │              │
 │                  │──cancelTransaction()───────────────────────────────>               │              │
 │                  │                  │                  │               │─────────────────────────────>refund($1.00)
 │                  │                  │                  │               │─resetTransaction()            │
 │                  │                  │                  │               │──────────────>showRefund($1.00)
 │                  │                  │                  │               │──────────────>showWelcome()   │
 │<──────────────────────────── $1.00 refunded ──────────────────────────────────────────────────────────│
```

### Flow 3: Insufficient Funds — Then Add More

```
User            ButtonPanel       VendingMachine       HasMoneyState     Display     PaymentProcessor
 │                  │                  │                    │               │              │
 │  (product already selected, $1.00 inserted, price=$2.00)│               │              │
 │                  │                  │                    │               │              │
 │─press Dispense─>│                  │                    │               │              │
 │                  │──dispense()──────────────────────────>│               │              │
 │                  │                  │                    │─balance<price │              │
 │                  │                  │                    │─throw InsufficientFundsException
 │                  │                  │  (caught at boundary, display error)│              │
 │                  │                  │                    │  (stays in HasMoneyState)    │
 │                  │                  │                    │               │              │
 │─insert $1.00───>│                  │                    │               │              │
 │                  │──insertMoney(1.00)───────────────────>│               │              │
 │                  │                  │                    │─addBalance()  │              │
 │                  │                  │                    │──────────────>showInsertMoney()
 │                  │                  │                    │──────────────>showMessage("Sufficient funds")
 │                  │                  │                    │               │              │
 │─press Dispense─>│                  │                    │               │              │
 │                  │──dispense()──────────────────────────>│               │              │
 │                  │                  │                    │─balance>=price│              │
 │                  │                  │                    │─────────────────────────────>charge($2.00)
 │                  │                  │                    │  ... (proceeds to dispensing) │
```

### Flow 4: Out of Stock

```
User            ButtonPanel       VendingMachine       IdleState       Inventory       Display
 │                  │                  │                  │               │              │
 │─press "A2"─────>│                  │                  │               │              │
 │                  │──selectProduct("A2")──────────────>│               │              │
 │                  │                  │                  │─isAvailable("A2")──>         │
 │                  │                  │                  │<──false──────│              │
 │                  │                  │                  │─────────────────────────────>showError("Out of stock")
 │                  │                  │                  │  (remains in IdleState)      │
 │<─────────────────────── "Product A2 is out of stock" ──────────────────────────────────│
```

### Flow 5: Invalid Actions (Exception-based)

```
User            ButtonPanel       VendingMachine       IdleState        Caller
 │                  │                  │                  │               │
 │─insert $1.00───>│  (no product selected)              │               │
 │                  │──insertMoney(1.00)────────────────>│               │
 │                  │                  │                  │─throw IllegalStateTransitionException("insert money", "IDLE")
 │                  │                  │                  │               │─catch → display error
 │                  │                  │                  │               │
 │─press Dispense─>│  (no product, no money)             │               │
 │                  │──dispense()──────────────────────>│               │
 │                  │                  │                  │─throw IllegalStateTransitionException("dispense", "IDLE")
 │                  │                  │                  │               │─catch → display error
```

### Flow 6: Concurrent Access (Thread Safety)

```
Thread-1          Thread-2          VendingMachine (synchronized)
  │                  │                  │
  │─selectProduct()─>│                  │
  │                  │─selectProduct()─>│
  │                  │                  │── Thread-1 acquires monitor
  │                  │                  │── Thread-1: select → HasMoney
  │                  │                  │── Thread-1 releases monitor
  │                  │                  │── Thread-2 acquires monitor
  │                  │                  │── Thread-2: throw IllegalStateTransition (already in HasMoney)
  │                  │                  │── Thread-2 releases monitor
  │─insertMoney()──>│                  │
  │                  │                  │── Thread-1 acquires monitor
  │                  │                  │── Thread-1: add balance
  │                  │                  │── ...serialized access continues...
```

---

## 5. State Transition Table

| Current State | Event | Condition | Action | Next State |
|---|---|---|---|---|
| Idle | selectProduct(code) | Stock available | Set product, show price | HasMoney |
| Idle | selectProduct(code) | Out of stock | Show error | Idle |
| Idle | insertMoney() | — | Throw `IllegalStateTransitionException` | Idle |
| Idle | dispense() | — | Throw `IllegalStateTransitionException` | Idle |
| Idle | cancel() | — | Show "no transaction" | Idle |
| HasMoney | selectProduct() | — | Throw `IllegalStateTransitionException` | HasMoney |
| HasMoney | insertMoney(amt) | — | Add to balance, show balance | HasMoney |
| HasMoney | dispense() | balance < price | Throw `InsufficientFundsException` | HasMoney |
| HasMoney | dispense() | balance >= price, payment ok | Charge, transition | Dispensing |
| HasMoney | dispense() | payment fails | Throw `PaymentFailedException` | HasMoney |
| HasMoney | cancel() | — | Refund balance, reset | Idle |
| Dispensing | dispense() | — | Dispense item, return change, reset | Idle |
| Dispensing | selectProduct() | — | Throw `IllegalStateTransitionException` | Dispensing |
| Dispensing | insertMoney() | — | Throw `IllegalStateTransitionException` | Dispensing |
| Dispensing | cancel() | — | Throw `IllegalStateTransitionException` | Dispensing |

---

## 6. Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `VendingMachine` (DCL + volatile) | One physical machine instance per JVM |
| **State** | `IdleState`, `HasMoneyState`, `DispensingState` | Clean transitions; invalid actions throw exceptions |
| **Strategy** | `PaymentProcessor`, `DispenseStrategy` | Swap payment/dispense behavior at runtime |
| **Command** | `Button` wrapping `Runnable` | Decouples button press from action execution |
| **Immutable Value** | `Product` (final, validated) | Thread-safe, no defensive copies needed |

---

## 7. Thread Safety Model

| Component | Mechanism | What It Protects |
|-----------|-----------|-----------------|
| `VendingMachine` user-facing methods | `synchronized(this)` | State machine transitions — only one thread can mutate transaction state |
| `VendingMachine` singleton init | DCL + `volatile` | Safe lazy initialization |
| `Inventory.racks` | `ConcurrentHashMap` | Admin add/remove concurrent with user reads |
| `Rack.quantity` | `AtomicInteger` + CAS loop | Lock-free stock decrement under contention |
| `ButtonPanel.productButtons` | `ConcurrentHashMap` | Dynamic button registration while panel is in use |
| Strategy setters | `synchronized` | Safe admin reconfiguration |

### Why this model (no redundant locks):
- Machine-level `synchronized` serializes user transactions. Within a transaction, no contention on state/balance/selectedProduct — only one thread touches them.
- `Inventory` uses `ConcurrentHashMap` (not RWLock) because operations are single-key lookups — no compound read-then-write that spans multiple keys.
- `Rack` uses `AtomicInteger` because the CAS-loop `dispense()` is a single atomic decrement — no need for a heavier lock.
- No double-locking. Each layer protects exactly what it owns.

---

## 8. File Structure

```
VendingMachine/
├── Main.java                            # Demo driver with all flows + concurrency test
├── exception/
│   ├── VendingMachineException.java     # Base exception
│   ├── InvalidProductException.java     # Unknown product code
│   ├── OutOfStockException.java         # Rack is empty
│   ├── InsufficientFundsException.java  # balance < price
│   ├── PaymentFailedException.java      # Processor returned false
│   └── IllegalStateTransitionException.java  # Invalid action in state
├── model/
│   ├── VendingMachine.java              # Core singleton, state machine, transaction state
│   ├── Button.java                      # Command pattern — label + Runnable
│   ├── ButtonPanel.java                 # Physical panel with dynamic product buttons
│   ├── Display.java                     # Stateless output renderer
│   ├── Inventory.java                   # ConcurrentHashMap of racks
│   ├── Rack.java                        # AtomicInteger quantity + CAS dispense
│   ├── Product.java                     # Immutable value object
│   └── AdminService.java               # Admin operations (add/remove/restock)
├── state/
│   ├── VendingMachineState.java         # State interface
│   ├── IdleState.java                   # Waiting for product selection
│   ├── HasMoneyState.java              # Accepting money / validating funds
│   └── DispensingState.java            # Releasing item + change
├── payment/
│   ├── PaymentProcessor.java            # Strategy interface (charge/refund)
│   ├── CashPaymentProcessor.java        # Physical cash handling
│   └── CardPaymentProcessor.java        # Card payment handling
└── dispense/
    ├── DispenseStrategy.java            # Strategy interface
    └── StandardDispenseStrategy.java    # Default rack dispense
```

---

## 9. Dependency Flow (No Cycles)

```
Main
 ├── model.VendingMachine
 │    ├── model.Inventory → model.Rack → model.Product
 │    ├── model.Display
 │    ├── model.ButtonPanel → model.Button
 │    ├── state.* (via interface, method param only)
 │    ├── payment.* (via interface)
 │    └── dispense.* (via interface)
 ├── model.AdminService → model.VendingMachine
 └── exception.* (thrown by states, caught at boundaries)
```

States reference `VendingMachine` only as a method parameter — no ownership, no circular dependency.

---

## 10. Key Design Decisions

1. **State belongs to the machine, not a "User"**: A vending machine transitions between idle/accepting/dispensing. The "user" is whoever is pressing buttons — they don't hold machine state. This eliminates the circular dependency (`Machine → User → State → Machine`).

2. **Exceptions over error codes**: Invalid state transitions throw typed exceptions (`IllegalStateTransitionException`). This makes the state contract explicit — callers must handle errors, and illegal transitions can't be silently ignored.

3. **Transaction state on the machine**: `balance` and `selectedProduct` are ephemeral transaction fields on `VendingMachine`, reset atomically via `resetTransaction()`. No separate "session" or "transaction" object needed for a single-user-at-a-time physical machine.

4. **CAS over locks for Rack quantity**: `AtomicInteger` with a compare-and-set loop is sufficient for a single-field atomic decrement. No need for a `synchronized` block or `ReentrantLock` — those would add contention in a concurrent-restock scenario.

5. **ConcurrentHashMap over RWLock for Inventory**: Single-key operations (get, put, remove) are inherently atomic in CHM. No compound operations span multiple keys, so RWLock adds complexity without benefit.

6. **Button as Command pattern**: Each `Button` wraps a `Runnable`. The panel doesn't know what actions do — it just stores and invokes commands. This means adding a new button type requires zero changes to `ButtonPanel`.

7. **Payment charged at dispense time, not insert time**: Money is accumulated (`addBalance`) and only processed via `PaymentProcessor.charge()` when the user confirms with Dispense. Cancellation simply resets balance and refunds — no reversal complexity.

8. **Double-dispatch for dispensing**: `HasMoneyState.dispense()` validates funds and charges payment, then transitions to `DispensingState` and immediately calls `dispense()` on it. `DispensingState.dispense()` does the physical dispensing. This separates payment validation from item release into distinct, single-responsibility states.

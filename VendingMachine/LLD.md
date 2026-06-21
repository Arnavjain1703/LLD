# Vending Machine — Low-Level Design (E2E)

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        VENDING MACHINE                           │
│                                                                 │
│  ┌──────────────┐    ┌──────────────────┐    ┌──────────────┐  │
│  │  ButtonPanel  │───>│  VendingMachine   │<───│ AdminService  │  │
│  │              │    │   (Singleton)     │    │              │  │
│  │ - Product Btn│    │                  │    │ - addProduct  │  │
│  │ - Insert Cash│    │  ┌────────────┐  │    │ - restock     │  │
│  │ - Dispense   │    │  │   State    │  │    │ - remove      │  │
│  │ - Cancel     │    │  │  Machine   │  │    └──────────────┘  │
│  │ - Display    │    │  └────────────┘  │                      │
│  └──────────────┘    │                  │                      │
│                      │  ┌────────────┐  │    ┌──────────────┐  │
│  ┌──────────────┐    │  │ Inventory  │  │    │  Payment     │  │
│  │   Display    │<───│  └────────────┘  │───>│  Processor   │  │
│  │              │    │                  │    │ (Strategy)   │  │
│  │ - Products   │    │  ┌────────────┐  │    └──────────────┘  │
│  │ - Balance    │    │  │  Dispense  │  │                      │
│  │ - Errors     │    │  │  Strategy  │  │                      │
│  │ - Messages   │    │  └────────────┘  │                      │
│  └──────────────┘    └──────────────────┘                      │
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
| `selectProduct()` | Validate stock -> move to HasMoney | Error: already selected | Error: wait |
| `insertMoney()` | Error: select first | Add to balance | Error: wait |
| `dispense()` | Error: select & pay | Check funds -> dispense or error | Execute dispense -> Idle |
| `cancel()` | No-op message | Refund balance -> Idle | Error: cannot cancel |

---

## 3. Component Details

### 3.1 ButtonPanel (User Input Interface)

Physical buttons the user interacts with. Each button delegates to `VendingMachine`.

| Button | Method | Triggers |
|--------|--------|----------|
| Product Code (A1, B1...) | `pressProductButton(code)` | `machine.selectProduct(code)` |
| Insert Cash | `pressInsertCash(amount)` | `machine.insertMoney(amount)` |
| Dispense | `pressDispense()` | `machine.dispense()` |
| Cancel | `pressCancel()` | `machine.cancelTransaction()` |
| Display Products | `pressDisplayProducts()` | `machine.displayProducts()` |

### 3.2 Display (Output Interface)

All user-facing output goes through `Display`:

| Method | When Shown |
|--------|-----------|
| `showWelcome()` | After reset to idle |
| `showProducts(inventory)` | User presses display button |
| `showMessage(msg)` | General info (selection confirmed, sufficient funds) |
| `showInsertMoney(balance, required)` | After product selection / money inserted |
| `showDispensing(productName)` | Item being dispensed |
| `showChange(amount)` | Overpaid, returning change |
| `showRefund(amount)` | Transaction cancelled |
| `showError(error)` | Invalid action or failure |

### 3.3 Payment Processor (Strategy Pattern)

```
PaymentProcessorInterface
├── CashPaymentProcessor   (physical coins/notes)
└── CardPaymentProcessor   (card tap/swipe)
```

Both implement:
- `process(amount)` — charge the user
- `refund(amount)` — return money (change or cancellation)

Machine can swap processor at runtime via `setPaymentProcessor()`.

### 3.4 Dispense Strategy (Strategy Pattern)

```
ItemDispenseStrategy
└── StandardDispenseStrategy   (decrement rack, release item)
```

Extensible for future strategies (e.g., fragile item handling, temperature-controlled).

---

## 4. E2E User Flows (Sequence Diagrams)

### Flow 1: Happy Path — Successful Purchase

```
User            ButtonPanel       VendingMachine     IdleState        HasMoneyState     DispensingState    Inventory    Display     PaymentProcessor
 │                  │                  │                │                  │                  │              │            │              │
 │─press Display───>│                  │                │                  │                  │              │            │              │
 │                  │──displayProducts()─>              │                  │                  │              │            │              │
 │                  │                  │                │                  │                  │──getReport()─>│            │              │
 │                  │                  │                │                  │                  │              │──showProducts()──>         │
 │                  │                  │                │                  │                  │              │            │              │
 │─press "A1"─────>│                  │                │                  │                  │              │            │              │
 │                  │──selectProduct("A1")──────────────>                  │                  │              │            │              │
 │                  │                  │                │─isAvailable("A1")────────────────────────────────>│            │              │
 │                  │                  │                │<──true───────────────────────────────────────────│            │              │
 │                  │                  │                │─setSelectedProduct()─>                │              │            │              │
 │                  │                  │                │─────────────────────────────────────────────────────────────>showMessage()     │
 │                  │                  │                │─────────────────────────────────────────────────────────────>showInsertMoney() │
 │                  │                  │                │─setState(HasMoney)──>                 │              │            │              │
 │                  │                  │                │                  │                  │              │            │              │
 │─insert $1.00───>│                  │                │                  │                  │              │            │              │
 │                  │──insertMoney(1.00)────────────────────────────────>  │                  │              │            │              │
 │                  │                  │                │                  │─addBalance(1.00)  │              │            │              │
 │                  │                  │                │                  │─────────────────────────────────────────────>showInsertMoney()
 │                  │                  │                │                  │                  │              │            │              │
 │─insert $1.00───>│                  │                │                  │                  │              │            │              │
 │                  │──insertMoney(1.00)────────────────────────────────>  │                  │              │            │              │
 │                  │                  │                │                  │─addBalance(1.00)  │              │            │              │
 │                  │                  │                │                  │─────────────────────────────────────────────>showInsertMoney()
 │                  │                  │                │                  │─────────────────────────────────────────────>showMessage("Sufficient funds")
 │                  │                  │                │                  │                  │              │            │              │
 │─press Dispense─>│                  │                │                  │                  │              │            │              │
 │                  │──dispense()──────────────────────────────────────>  │                  │              │            │              │
 │                  │                  │                │                  │─balance>=price?  │              │            │              │
 │                  │                  │                │                  │──────────────────────────────────────────────────────────>process(1.50)
 │                  │                  │                │                  │<─────────────────────────────────────────────────────────true
 │                  │                  │                │                  │─setState(Dispensing)───────────>│              │            │
 │                  │                  │                │                  │                  │              │            │              │
 │                  │                  │──dispense()────────────────────────────────────────>│              │            │              │
 │                  │                  │                │                  │                  │─dispenseItem()─>           │              │
 │                  │                  │                │                  │                  │──────────────────────────>showDispensing()│
 │                  │                  │                │                  │                  │──────────────────────────>showChange($0.50)
 │                  │                  │                │                  │                  │─setState(Idle)─>           │              │
 │                  │                  │                │                  │                  │──────────────────────────>showWelcome()   │
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
 │                  │                  │                  │               │─resetBalance() │              │
 │                  │                  │                  │               │─setSelectedProduct(null)       │
 │                  │                  │                  │               │─setState(Idle) │              │
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
 │                  │                  │                    │──────────────>showError("Insufficient funds")
 │                  │                  │                    │──────────────>showMessage("Insert more or CANCEL")
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
 │                  │                  │                    │─────────────────────────────>process($2.00)
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
 │                  │                  │                  │─────────────────────────────>showWelcome()
 │                  │                  │                  │  (remains in IdleState)      │
 │<─────────────────────── "Product A2 is out of stock" ──────────────────────────────────│
```

### Flow 5: Invalid Actions (Error Handling)

```
User            ButtonPanel       VendingMachine       IdleState        Display
 │                  │                  │                  │               │
 │─insert $1.00───>│  (no product selected)              │               │
 │                  │──insertMoney(1.00)────────────────>│               │
 │                  │                  │                  │──────────────>showError("Select product first")
 │                  │                  │                  │               │
 │─press Dispense─>│  (no product, no money)             │               │
 │                  │──dispense()──────────────────────>│               │
 │                  │                  │                  │──────────────>showError("Select product and insert money")
 │                  │                  │                  │               │
 │─press Cancel───>│  (no transaction)                   │               │
 │                  │──cancelTransaction()──────────────>│               │
 │                  │                  │                  │──────────────>showMessage("No transaction in progress")
```

### Flow 6: Card Payment

```
User            ButtonPanel       VendingMachine       HasMoneyState    CardPaymentProcessor    Display
 │                  │                  │                    │                  │                  │
 │  (Admin sets CardPaymentProcessor on machine)           │                  │                  │
 │                  │                  │                    │                  │                  │
 │─press "C1"─────>│                  │                    │                  │                  │
 │                  │──selectProduct("C1")──>               │                  │                  │
 │                  │                  │  (transitions to HasMoney)            │                  │
 │                  │                  │                    │                  │                  │
 │─insert $2.00───>│                  │                    │                  │                  │
 │                  │──insertMoney(2.00)───────────────────>│                  │                  │
 │                  │                  │                    │                  │                  │
 │─press Dispense─>│                  │                    │                  │                  │
 │                  │──dispense()──────────────────────────>│                  │                  │
 │                  │                  │                    │─────────────────>process($1.00)     │
 │                  │                  │                    │<────────────────true                │
 │                  │                  │                    │  ... (dispense + change via card refund)
 │                  │                  │                    │─────────────────>refund($1.00)      │
```

### Flow 7: Re-selection During Transaction (Blocked)

```
User            ButtonPanel       VendingMachine       HasMoneyState     Display
 │                  │                  │                    │               │
 │  (C1 selected, $0.50 inserted)      │                    │               │
 │                  │                  │                    │               │
 │─press "B1"─────>│                  │                    │               │
 │                  │──selectProduct("B1")─────────────────>│               │
 │                  │                  │                    │──────────────>showError("Product already selected")
 │                  │                  │                    │  (stays in HasMoneyState with C1)   │
 │                  │                  │                    │               │
 │─press Cancel───>│  (must cancel first to reselect)      │               │
 │                  │──cancelTransaction()─────────────────>│               │
 │                  │                  │                    │  ... (refund, reset to Idle)        │
```

---

## 5. State Transition Table

| Current State | Event | Condition | Action | Next State |
|---|---|---|---|---|
| Idle | selectProduct(code) | Stock available | Set product, show price | HasMoney |
| Idle | selectProduct(code) | Out of stock | Show error | Idle |
| Idle | insertMoney() | — | Show error | Idle |
| Idle | dispense() | — | Show error | Idle |
| Idle | cancel() | — | Show "no transaction" | Idle |
| HasMoney | selectProduct() | — | Show error (already selected) | HasMoney |
| HasMoney | insertMoney(amt) | — | Add to balance, show balance | HasMoney |
| HasMoney | dispense() | balance < price | Show insufficient funds | HasMoney |
| HasMoney | dispense() | balance >= price | Process payment, transition | Dispensing |
| HasMoney | cancel() | — | Refund balance, clear selection | Idle |
| Dispensing | dispense() | — | Dispense item, return change, reset | Idle |
| Dispensing | selectProduct() | — | Show error (wait) | Dispensing |
| Dispensing | insertMoney() | — | Show error (wait) | Dispensing |
| Dispensing | cancel() | — | Show error (cannot cancel) | Dispensing |

---

## 6. Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `VendingMachine` | One physical machine instance |
| **State** | `IdleState`, `HasMoneyState`, `DispensingState` | Clean transitions without conditionals |
| **Strategy** | `PaymentProcessorInterface`, `ItemDispenseStrategy` | Swap payment/dispense behavior at runtime |
| **Facade** | `ButtonPanel` | Simple interface hiding machine complexity |
| **Composition** | `VendingMachine -> Inventory -> Rack -> Product` | Strong ownership hierarchy |

---

## 7. Thread Safety

| Component | Mechanism | Reason |
|-----------|-----------|--------|
| `VendingMachine` | `synchronized` methods | Single user interacts at a time |
| `VendingMachine` (singleton) | Double-checked locking + `volatile` | Safe lazy init |
| `Inventory` | `ReentrantReadWriteLock` | Admin reads/writes concurrent with user reads |
| `Rack` | `synchronized` methods | Atomic quantity operations |

---

## 8. File Structure

```
VendingMachine/
├── Main.java                          # Demo driver with all flows
├── enums/
│   ├── MachineStateType.java          # IDLE, HAS_MONEY, DISPENSING
│   └── PaymentMethod.java             # CASH, CARD
└── models/
    ├── VendingMachine.java            # Core singleton, delegates to state
    ├── ButtonPanel.java               # User input facade
    ├── Display.java                   # All user-facing output
    ├── Inventory.java                 # Thread-safe rack management
    ├── Rack.java                      # Slot holding product + quantity
    ├── Product.java                   # Name, price, code
    ├── AdminService.java              # Admin operations
    ├── State/
    │   ├── VendingMachineState.java   # State interface
    │   ├── IdleState.java             # Waiting for selection
    │   ├── HasMoneyState.java         # Accepting money / ready to dispense
    │   └── DispensingState.java       # Releasing item
    ├── PaymentProcessor/
    │   ├── PaymentProcessorInterface.java  # Strategy interface
    │   ├── CashPaymentProcessor.java       # Physical cash
    │   └── CardPaymentProcessor.java       # Card payment
    └── ItemDispenseStrategy/
        ├── ItemDispenseStrategy.java       # Strategy interface
        └── StandardDispenseStrategy.java   # Default rack dispense
```

---

## 9. Key Design Decisions

1. **State pattern over switch-case**: Each state encapsulates its own transition logic. Adding a new state (e.g., `MaintenanceState`) requires zero changes to existing states.

2. **ButtonPanel as facade**: Users don't interact with the machine directly. The panel validates nothing — it simply translates physical button presses into machine method calls. The state handles validation.

3. **Display is passive**: The machine/state pushes messages to Display. Display never queries state — it just renders what it's told.

4. **Payment processed at dispense time, not insert time**: Money is accumulated (`addBalance`) and only charged via the processor when the user confirms with Dispense. This simplifies cancellation (just reset balance).

5. **Cancellation always returns to Idle**: No partial states. Cancel clears everything — selected product, balance — and refunds via the active payment processor.

6. **Double-dispatch for dispensing**: `HasMoneyState.dispense()` validates funds and transitions to `DispensingState`, then calls `machine.dispense()` again. The `DispensingState.dispense()` does the actual physical dispensing. This separates payment validation from item release.

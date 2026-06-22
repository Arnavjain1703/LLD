# Vending Machine — Thread-Safe LLD

## Design Decisions

| Concern | Decision |
|---|---|
| FSM | State pattern — each state enforces its own valid transitions |
| Payment | Strategy pattern — `CashPaymentStrategy` and `CardPaymentStrategy` are interchangeable |
| Thread safety | `ReentrantLock(fair=true)` in `VendingMachine` for FSM transitions |
| Inventory | `ConcurrentHashMap<Product, AtomicInteger>` + CAS loop — no global lock for deductions |
| Cash coins | `AtomicLong` (cents) — lock-free coin accumulation |
| Card token | `AtomicReference<String>` — safe card insertion/removal |
| Price precision | All amounts stored as **cents (long)** — avoids floating-point errors |
| Card network | `CardProcessor` interface + `SimulatedCardProcessor` — injectable/mockable |

---

## State Machine Diagram

```mermaid
stateDiagram-v2
    [*] --> IDLE

    IDLE --> PRODUCT_SELECTED : selectProduct()\n[item in stock]
    IDLE --> IDLE : cancel()\n[no-op]

    PRODUCT_SELECTED --> PAYMENT_PENDING : selectPaymentMethod(CASH)\nselectPaymentMethod(CARD)
    PRODUCT_SELECTED --> IDLE : cancel()\n[no charge]
    PRODUCT_SELECTED --> PRODUCT_SELECTED : selectProduct()\n[re-select]

    PAYMENT_PENDING --> PAYMENT_PENDING : insertCoin()\n[cash — still underpaid]
    PAYMENT_PENDING --> PAYMENT_PENDING : swipeCard()\n[card declined — retry]
    PAYMENT_PENDING --> DISPENSING : insertCoin()\n[cash — total >= price]
    PAYMENT_PENDING --> DISPENSING : swipeCard()\n[card approved]
    PAYMENT_PENDING --> IDLE : cancel()\n[refund issued]

    DISPENSING --> IDLE : dispenseProduct()\n[give item + change]
```

---

## Class Diagram

```mermaid
classDiagram
    class VendingMachine {
        -VendingMachine instance$
        -ReentrantLock lock
        -VendingMachineState currentState
        -Product selectedProduct
        -PaymentStrategy paymentStrategy
        -Inventory inventory
        +getInstance() VendingMachine$
        +selectProduct(Product)
        +selectPaymentMethod(PaymentStrategy)
        +insertCoin(Coin)
        +swipeCard(String)
        +cancel()
        +addProduct(Product, int)
        ~dispenseProduct()
        ~resetToIdle()
    }

    class VendingMachineState {
        <<interface>>
        +selectProduct(VendingMachine, Product)
        +selectPaymentMethod(VendingMachine, PaymentStrategy)
        +insertCoin(VendingMachine, Coin)
        +swipeCard(VendingMachine, String)
        +cancel(VendingMachine)
        +getStateName() String
    }

    class IdleState { +getStateName() String }
    class ProductSelectedState { +getStateName() String }
    class PaymentPendingState { +getStateName() String }
    class DispensingState { +getStateName() String }

    VendingMachineState <|.. IdleState
    VendingMachineState <|.. ProductSelectedState
    VendingMachineState <|.. PaymentPendingState
    VendingMachineState <|.. DispensingState
    VendingMachine o-- VendingMachineState

    class PaymentStrategy {
        <<interface>>
        +getType() PaymentType
        +processPayment(long) bool
        +collectChangeInCents(long) long
        +refund() long
    }

    class CashPaymentStrategy {
        -AtomicLong insertedCents
        +insertCoin(Coin)
        +getInsertedCents() long
        +processPayment(long) bool
        +collectChangeInCents(long) long
        +refund() long
    }

    class CardPaymentStrategy {
        -CardProcessor processor
        -AtomicReference~String~ cardToken
        -volatile long lastChargedCents
        +swipeCard(String)
        +isCardInserted() bool
        +processPayment(long) bool
        +collectChangeInCents(long) long
        +refund() long
    }

    class CardProcessor {
        <<interface>>
        +tokenize(String) String
        +charge(String, long) bool
        +refund(String, long)
    }

    class SimulatedCardProcessor {
        -Set~String~ blockedTokens
        +tokenize(String) String
        +charge(String, long) bool
        +refund(String, long)
    }

    PaymentStrategy <|.. CashPaymentStrategy
    PaymentStrategy <|.. CardPaymentStrategy
    CardProcessor <|.. SimulatedCardProcessor
    CardPaymentStrategy --> CardProcessor
    VendingMachine o-- PaymentStrategy

    class Inventory {
        -ConcurrentHashMap~Product,AtomicInteger~ stock
        +addProduct(Product, int)
        +isAvailable(Product) bool
        +deduct(Product) bool
        +getQuantity(Product) int
        +getSnapshot() Map
    }

    class Product {
        -String productId
        -String name
        -long priceInCents
        +getPrice() double
        +getPriceInCents() long
    }

    class Coin {
        <<enum>>
        PENNY NICKEL DIME QUARTER ONE FIVE
        -long valueInCents
        +getValueInCents() long
    }

    VendingMachine --> Inventory
    Inventory --> Product
    CashPaymentStrategy --> Coin
```

---

## Payment Flow Sequence

```mermaid
sequenceDiagram
    actor User
    participant VM as VendingMachine
    participant State as PaymentPendingState
    participant Pay as PaymentStrategy
    participant Inv as Inventory

    User->>VM: selectProduct("Coke")
    VM->>State: (transitions to ProductSelectedState)

    alt Cash Payment
        User->>VM: selectPaymentMethod(CashPaymentStrategy)
        VM->>State: (transitions to PaymentPendingState)
        loop Until inserted >= price
            User->>VM: insertCoin(QUARTER)
            VM->>Pay: insertCoin(QUARTER)
            Pay-->>VM: inserted total
        end
        VM->>Pay: processPayment(price)
        Pay-->>VM: true
        VM->>State: (transitions to DispensingState)
        VM->>Inv: deduct(product) [CAS]
        VM->>Pay: collectChangeInCents(price)
        Pay-->>VM: change amount
        VM-->>User: dispense product + change
    else Card Payment
        User->>VM: selectPaymentMethod(CardPaymentStrategy)
        VM->>State: (transitions to PaymentPendingState)
        User->>VM: swipeCard("4111...")
        VM->>Pay: swipeCard → tokenize
        VM->>Pay: processPayment(price)
        Pay->>CardProcessor: charge(token, amount)
        CardProcessor-->>Pay: approved
        VM->>State: (transitions to DispensingState)
        VM->>Inv: deduct(product) [CAS]
        VM-->>User: dispense product (no change)
    end

    VM->>State: resetToIdle()
```

---

## Thread-Safety Guarantees

| Layer | Mechanism | Protects |
|---|---|---|
| FSM transitions | `ReentrantLock(fair)` in `VendingMachine` | Prevents two threads from concurrently transitioning states |
| Coin accumulation | `AtomicLong` CAS in `CashPaymentStrategy` | No lost coin insertions under concurrent access |
| Card token | `AtomicReference<String>` | Safe concurrent swipe/cancel |
| Inventory deduction | `AtomicInteger` CAS loop in `Inventory.deduct()` | Prevents double-dispense of the last item even if lock is released between check and deduct |

> The CAS loop in `Inventory.deduct()` is the critical double-safety: even if the `ReentrantLock`
> in `VendingMachine` serializes most flows, the inventory guard catches the race where two
> concurrent sessions both reach `dispenseProduct()` for the last unit in stock.

---

## Project Structure

```
src/main/java/vendingmachine/
├── VendingMachine.java              # Singleton, ReentrantLock, FSM orchestration
├── Main.java                        # Demo: 6 scenarios
├── model/
│   ├── Product.java                 # productId, name, priceInCents
│   └── Coin.java                    # Enum: PENNY..FIVE (valueInCents)
├── payment/
│   ├── PaymentStrategy.java         # Strategy interface
│   ├── PaymentType.java             # Enum: CASH, CARD
│   ├── CashPaymentStrategy.java     # AtomicLong coin tracking
│   ├── CardPaymentStrategy.java     # AtomicReference token + CardProcessor
│   ├── CardProcessor.java           # Interface for card network
│   └── SimulatedCardProcessor.java  # Test/demo implementation
├── inventory/
│   └── Inventory.java               # ConcurrentHashMap + AtomicInteger CAS
├── state/
│   ├── VendingMachineState.java     # Interface
│   ├── IdleState.java
│   ├── ProductSelectedState.java
│   ├── PaymentPendingState.java     # Handles both cash & card paths
│   └── DispensingState.java
└── exception/
    ├── InsufficientFundsException.java
    ├── OutOfStockException.java
    ├── InvalidStateException.java
    └── CardDeclinedException.java
```
# Vending Machine — Class Diagram

```mermaid
classDiagram

    %% ─── CORE ───────────────────────────────────────────────

    class VendingMachine {
        <<Singleton>>
        -String machineId
        -VendingMachineState currentState
        -Inventory inventory
        -double currentBalance
        -Product selectedProduct
        +getInstance() VendingMachine
        +selectProduct(String code)
        +insertMoney(double amount)
        +dispense()
        +cancelTransaction()
        +getBalance() double
        +setState(VendingMachineState)
        +getState() VendingMachineState
    }

    %% ─── INVENTORY ─────────────────────────────────────────

    class Inventory {
        -Map~String, Rack~ racks
        +addRack(Rack)
        +removeRack(String code)
        +getRack(String code) Rack
        +isAvailable(String code) boolean
        +getProduct(String code) Product
        +dispenseItem(String code)
        +restock(String code, int quantity)
        +getReport() Map
    }

    class Rack {
        -String code
        -Product product
        -int quantity
        -int maxCapacity
        +isAvailable() boolean
        +dispense()
        +restock(int quantity)
        +getProduct() Product
        +getQuantity() int
    }

    class Product {
        -String name
        -double price
        -String code
        +getName() String
        +getPrice() double
        +getCode() String
    }

    %% ─── STATE PATTERN ─────────────────────────────────────

    class VendingMachineState {
        <<interface>>
        +selectProduct(VendingMachine, String code)
        +insertMoney(VendingMachine, double amount)
        +dispense(VendingMachine)
        +cancel(VendingMachine)
    }

    class IdleState {
        +selectProduct(VendingMachine, String code)
        +insertMoney(VendingMachine, double amount)
        +dispense(VendingMachine)
        +cancel(VendingMachine)
    }

    class HasMoneyState {
        +selectProduct(VendingMachine, String code)
        +insertMoney(VendingMachine, double amount)
        +dispense(VendingMachine)
        +cancel(VendingMachine)
    }

    class DispensingState {
        +selectProduct(VendingMachine, String code)
        +insertMoney(VendingMachine, double amount)
        +dispense(VendingMachine)
        +cancel(VendingMachine)
    }

    %% ─── PAYMENT ───────────────────────────────────────────

    class PaymentProcessor {
        <<interface>>
        +process(double amount) boolean
        +refund(double amount) boolean
    }

    class CashPaymentProcessor {
        +process(double amount) boolean
        +refund(double amount) boolean
    }

    class CardPaymentProcessor {
        +process(double amount) boolean
        +refund(double amount) boolean
    }

    %% ─── DISPENSING STRATEGY ───────────────────────────────

    class ItemDispenseStrategy {
        <<interface>>
        +dispense(Rack rack)
    }

    class StandardDispenseStrategy {
        +dispense(Rack rack)
    }

    %% ─── ADMIN ─────────────────────────────────────────────

    class AdminService {
        -VendingMachine machine
        +addProduct(String code, Product product, int quantity)
        +removeProduct(String code)
        +restock(String code, int quantity)
        +collectCash() double
        +getInventoryReport() Map
    }

    %% ─── ENUMS ─────────────────────────────────────────────

    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
    }

    class MachineStateType {
        <<enumeration>>
        IDLE
        HAS_MONEY
        DISPENSING
    }

    %% ─── RELATIONSHIPS ─────────────────────────────────────

    VendingMachine "1" *-- "1" Inventory
    VendingMachine --> VendingMachineState
    VendingMachine --> Product : selectedProduct

    Inventory "1" *-- "many" Rack
    Rack "1" *-- "1" Product

    VendingMachineState <|.. IdleState
    VendingMachineState <|.. HasMoneyState
    VendingMachineState <|.. DispensingState

    PaymentProcessor <|.. CashPaymentProcessor
    PaymentProcessor <|.. CardPaymentProcessor

    ItemDispenseStrategy <|.. StandardDispenseStrategy

    VendingMachine ..> PaymentProcessor : uses
    VendingMachine ..> ItemDispenseStrategy : uses

    AdminService --> VendingMachine
```

---

## Relationship Legend

| Symbol | Type | Meaning | Lifecycle Dependency |
|--------|------|---------|----------------------|
| `*--` | Composition | Strong ownership — child is created and destroyed with parent | Child dies with parent |
| `o--` | Aggregation | Weak ownership — parent holds child but child exists independently | Child survives parent |
| `<\|--` | Inheritance | "is-a" — subclass extends parent | — |
| `<\|..` | Realization | "implements" — class fulfills interface contract | — |
| `-->` | Association | "has-a" — holds a long-term reference | Independent |
| `..>` | Dependency | "uses-a" — short-term, method-level usage | Independent |

---

## Relationships Applied

### Composition `*--`
| Relationship | Reason |
|---|---|
| `VendingMachine *-- Inventory` | Inventory has no meaning outside the machine |
| `Inventory *-- Rack` | Racks belong to the inventory |
| `Rack *-- Product` | Product definition is owned by the rack |

### Realization `<|..`
| Relationship | Reason |
|---|---|
| `VendingMachineState <\|.. IdleState / HasMoneyState / DispensingState` | Each state implements the state interface |
| `PaymentProcessor <\|.. CashPaymentProcessor / CardPaymentProcessor` | Each implements payment processing |
| `ItemDispenseStrategy <\|.. StandardDispenseStrategy` | Implements dispensing behavior |

### Association `-->`
| Relationship | Reason |
|---|---|
| `VendingMachine --> VendingMachineState` | Machine holds current state reference |
| `VendingMachine --> Product` | Machine holds reference to currently selected product |
| `AdminService --> VendingMachine` | Admin holds persistent reference to machine |

### Dependency `..>`
| Relationship | Reason |
|---|---|
| `VendingMachine ..> PaymentProcessor` | Machine uses processor during transaction |
| `VendingMachine ..> ItemDispenseStrategy` | Machine uses strategy during dispensing |

# Vending Machine — Class Diagram

```mermaid
classDiagram

    %% ─── CORE ───────────────────────────────────────────────

    class VendingMachine {
        <<Singleton>>
        -static volatile VendingMachine instance
        -String machineId
        -Inventory inventory
        -Display display
        -PaymentProcessorInterface paymentProcessor
        -ItemDispenseStrategy dispenseStrategy
        -User currentUser
        +static getInstance(String machineId) VendingMachine
        +synchronized displayProducts()
        +synchronized selectProduct(String code)
        +synchronized insertMoney(double amount)
        +synchronized dispense()
        +synchronized cancelTransaction()
        +getInventory() Inventory
        +getDisplay() Display
        +getPaymentProcessor() PaymentProcessorInterface
        +setPaymentProcessor(PaymentProcessorInterface)
        +getDispenseStrategy() ItemDispenseStrategy
        +setDispenseStrategy(ItemDispenseStrategy)
    }

    class User {
        -VendingMachineState currentState
        -double currentBalance
        -Product selectedProduct
        +getState() VendingMachineState
        +setState(VendingMachineState)
        +getBalance() double
        +addBalance(double amount)
        +resetBalance()
        +getSelectedProduct() Product
        +setSelectedProduct(Product)
        +isActive() boolean
    }

    %% ─── USER INTERFACE ────────────────────────────────────

    class ButtonPanel {
        -VendingMachine machine
        +ButtonPanel(VendingMachine)
        +pressProductButton(String code)
        +pressInsertCash(double amount)
        +pressDispense()
        +pressCancel()
        +pressDisplayProducts()
    }

    class Display {
        -String currentMessage
        +Display()
        +showWelcome()
        +showProducts(Inventory)
        +showMessage(String message)
        +showInsertMoney(double balance, double required)
        +showDispensing(String productName)
        +showChange(double amount)
        +showRefund(double amount)
        +showError(String error)
        -render()
    }

    %% ─── INVENTORY ─────────────────────────────────────────

    class Inventory {
        -Map~String, Rack~ racks
        -ReentrantReadWriteLock rwLock
        +addRack(Rack)
        +removeRack(String code)
        +getRack(String code) Rack
        +isAvailable(String code) boolean
        +getProduct(String code) Product
        +dispenseItem(String code)
        +restock(String code, int quantity)
        +getReport() Map
        +getAllRacks() Map~String, Rack~
    }

    class Rack {
        -String code
        -Product product
        -int quantity
        -int maxCapacity
        +synchronized isAvailable() boolean
        +synchronized dispense()
        +synchronized restock(int amount)
        +getProduct() Product
        +synchronized getQuantity() int
        +getCode() String
        +getMaxCapacity() int
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
        +selectProduct(VendingMachine, User, String code)
        +insertMoney(VendingMachine, User, double amount)
        +dispense(VendingMachine, User)
        +cancel(VendingMachine, User)
    }

    class IdleState {
        +selectProduct(VendingMachine, User, String code)
        +insertMoney(VendingMachine, User, double amount)
        +dispense(VendingMachine, User)
        +cancel(VendingMachine, User)
    }

    class HasMoneyState {
        +selectProduct(VendingMachine, User, String code)
        +insertMoney(VendingMachine, User, double amount)
        +dispense(VendingMachine, User)
        +cancel(VendingMachine, User)
    }

    class DispensingState {
        +selectProduct(VendingMachine, User, String code)
        +insertMoney(VendingMachine, User, double amount)
        +dispense(VendingMachine, User)
        +cancel(VendingMachine, User)
    }

    %% ─── PAYMENT (Strategy) ───────────────────────────────

    class PaymentProcessorInterface {
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
        +AdminService(VendingMachine)
        +addProduct(String code, Product product, int quantity, int maxCapacity)
        +removeProduct(String code)
        +restock(String code, int quantity)
        +getInventoryReport()
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
    VendingMachine "1" *-- "1" Display
    VendingMachine "1" *-- "1" User

    User --> VendingMachineState : currentState
    User --> Product : selectedProduct

    Inventory "1" *-- "many" Rack
    Rack "1" *-- "1" Product

    VendingMachineState <|.. IdleState
    VendingMachineState <|.. HasMoneyState
    VendingMachineState <|.. DispensingState

    PaymentProcessorInterface <|.. CashPaymentProcessor
    PaymentProcessorInterface <|.. CardPaymentProcessor

    ItemDispenseStrategy <|.. StandardDispenseStrategy

    VendingMachine ..> PaymentProcessorInterface : uses
    VendingMachine ..> ItemDispenseStrategy : uses

    ButtonPanel --> VendingMachine : delegates to
    AdminService --> VendingMachine : manages
```

---

## Relationship Legend

| Symbol | Type | Meaning | Lifecycle Dependency |
|--------|------|---------|----------------------|
| `*--` | Composition | Strong ownership — child is created and destroyed with parent | Child dies with parent |
| `o--` | Aggregation | Weak ownership — parent holds child but child exists independently | Child survives parent |
| `<|--` | Inheritance | "is-a" — subclass extends parent | — |
| `<|..` | Realization | "implements" — class fulfills interface contract | — |
| `-->` | Association | "has-a" — holds a long-term reference | Independent |
| `..>` | Dependency | "uses-a" — short-term, method-level usage | Independent |

---

## Relationships Applied

### Composition `*--`
| Relationship | Reason |
|---|---|
| `VendingMachine *-- Inventory` | Inventory has no meaning outside the machine |
| `VendingMachine *-- Display` | Display is owned by and exists only within the machine |
| `VendingMachine *-- User` | User is the current user session, owned by machine |
| `Inventory *-- Rack` | Racks belong to the inventory |
| `Rack *-- Product` | Product definition is owned by the rack |

### Realization `<|..`
| Relationship | Reason |
|---|---|
| `VendingMachineState <|.. IdleState / HasMoneyState / DispensingState` | Each state implements the state interface |
| `PaymentProcessorInterface <|.. CashPaymentProcessor / CardPaymentProcessor` | Each implements payment processing |
| `ItemDispenseStrategy <|.. StandardDispenseStrategy` | Implements dispensing behavior |

### Association `-->`
| Relationship | Reason |
|---|---|
| `User --> VendingMachineState` | User holds current state reference (swapped at runtime) |
| `User --> Product` | User holds reference to currently selected product |
| `ButtonPanel --> VendingMachine` | Panel holds persistent reference, delegates all user actions |
| `AdminService --> VendingMachine` | Admin holds persistent reference to machine for management ops |

### Dependency `..>`
| Relationship | Reason |
|---|---|
| `VendingMachine ..> PaymentProcessorInterface` | Machine uses processor during transaction (swappable strategy) |
| `VendingMachine ..> ItemDispenseStrategy` | Machine uses strategy during dispensing (swappable strategy) |

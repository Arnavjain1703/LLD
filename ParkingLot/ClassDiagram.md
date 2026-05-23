# Parking Lot — Class Diagram

```mermaid
classDiagram

    %% ─── CORE ───────────────────────────────────────────────

    class ParkingLot {
        <<Singleton>>
        -String id
        -String name
        -String address
        -List~Floor~ floors
        -List~EntryGate~ entryGates
        -List~ExitGate~ exitGates
        +getInstance() ParkingLot
        +addFloor(Floor)
        +removeFloor(int floorNumber)
        +addEntryGate(EntryGate)
        +removeEntryGate(String gateId)
        +addExitGate(ExitGate)
        +removeExitGate(String gateId)
        +getAvailability() Map
    }

    class Floor {
        -int floorNumber
        -List~ParkingSpot~ spots
        -DisplayBoard displayBoard
        +addSpot(ParkingSpot)
        +removeSpot(String spotId)
        +getAvailableSpots(VehicleType) List~ParkingSpot~
        +getOccupancyReport() OccupancyReport
    }

    class DisplayBoard {
        -String boardId
        +showAvailability()
        +update()
    }

    %% ─── SPOTS ──────────────────────────────────────────────

    class ParkingSpot {
        <<abstract>>
        -String spotId
        -SpotType spotType
        -boolean isOccupied
        -Vehicle vehicle
        +canFit(Vehicle) boolean
        +park(Vehicle)
        +free()
    }

    class MotorcycleSpot {
        +canFit(Vehicle) boolean
    }
    class CompactSpot {
        +canFit(Vehicle) boolean
    }
    class LargeSpot {
        +canFit(Vehicle) boolean
    }
    class EVSpot {
        -boolean hasCharger
        +canFit(Vehicle) boolean
    }
    class HandicappedSpot {
        +canFit(Vehicle) boolean
    }

    %% ─── VEHICLES ───────────────────────────────────────────

    class Vehicle {
        <<abstract>>
        -String licensePlate
        -VehicleType vehicleType
    }

    class Bike
    class Car
    class Truck
    class EVCar
    class HandicappedVehicle

    %% ─── GATES ──────────────────────────────────────────────

    class EntryGate {
        -String gateId
        -SpotAssignmentStrategy assignmentStrategy
        +processEntry(Vehicle) Ticket
    }

    class ExitGate {
        -String gateId
        -FeeCalculator feeCalculator
        +processExit(Ticket) Payment
    }

    %% ─── TICKET & PAYMENT ───────────────────────────────────

    class Ticket {
        -String ticketId
        -Vehicle vehicle
        -ParkingSpot spot
        -LocalDateTime entryTime
        -LocalDateTime exitTime
        -TicketStatus status
    }

    class Payment {
        -String paymentId
        -Ticket ticket
        -double amount
        -PaymentMethod method
        -PaymentStatus status
        +processPayment() boolean
    }

    %% ─── STRATEGIES ─────────────────────────────────────────

    class SpotAssignmentStrategy {
        <<interface>>
        +assignSpot(Vehicle, List~Floor~) ParkingSpot
    }
    class NearestSpotStrategy {
        +assignSpot(Vehicle, List~Floor~) ParkingSpot
    }
    class RandomSpotStrategy {
        +assignSpot(Vehicle, List~Floor~) ParkingSpot
    }
    class FloorPreferenceStrategy {
        +assignSpot(Vehicle, List~Floor~) ParkingSpot
    }

    class FeeCalculator {
        <<interface>>
        +calculate(Ticket) double
    }
    class HourlyFeeStrategy {
        -Map~VehicleType, Double~ ratePerHour
        +calculate(Ticket) double
    }
    class DailyFeeStrategy {
        -Map~VehicleType, Double~ ratePerDay
        +calculate(Ticket) double
    }

    class PaymentProcessor {
        <<interface>>
        +process(double amount) boolean
    }
    class CashPaymentProcessor {
        +process(double amount) boolean
    }
    class CardPaymentProcessor {
        +process(double amount) boolean
    }

    %% ─── ADMIN ──────────────────────────────────────────────

    class AdminService {
        -ParkingLot parkingLot
        +addFloor(Floor)
        +removeFloor(int floorNumber)
        +addSpot(int floorNumber, ParkingSpot)
        +removeSpot(int floorNumber, String spotId)
        +addEntryGate(EntryGate)
        +removeEntryGate(String gateId)
        +addExitGate(ExitGate)
        +removeExitGate(String gateId)
        +updatePricingStrategy(FeeCalculator)
        +getOccupancyReport() OccupancyReport
    }

    %% ─── RELATIONSHIPS ──────────────────────────────────────

    ParkingLot "1" *-- "many" Floor
    ParkingLot "1" *-- "many" EntryGate
    ParkingLot "1" *-- "many" ExitGate

    Floor "1" *-- "many" ParkingSpot
    Floor "1" *-- "1" DisplayBoard

    ParkingSpot <|-- MotorcycleSpot
    ParkingSpot <|-- CompactSpot
    ParkingSpot <|-- LargeSpot
    ParkingSpot <|-- EVSpot
    ParkingSpot <|-- HandicappedSpot

    Vehicle <|-- Bike
    Vehicle <|-- Car
    Vehicle <|-- Truck
    Vehicle <|-- EVCar
    Vehicle <|-- HandicappedVehicle

    ParkingSpot "1" o-- "0..1" Vehicle

    EntryGate --> SpotAssignmentStrategy
    ExitGate --> FeeCalculator

    EntryGate ..> Ticket : creates
    ExitGate ..> Payment : creates

    Ticket --> Vehicle
    Ticket --> ParkingSpot

    Payment --> Ticket
    Payment ..> PaymentProcessor

    SpotAssignmentStrategy <|.. NearestSpotStrategy
    SpotAssignmentStrategy <|.. RandomSpotStrategy
    SpotAssignmentStrategy <|.. FloorPreferenceStrategy

    FeeCalculator <|.. HourlyFeeStrategy
    FeeCalculator <|.. DailyFeeStrategy

    PaymentProcessor <|.. CashPaymentProcessor
    PaymentProcessor <|.. CardPaymentProcessor

    AdminService --> ParkingLot
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
| `..`>` | Dependency | "uses-a" — short-term, method-level usage | Independent |

---

## Relationships Applied

### Composition `*--`
| Relationship | Reason |
|---|---|
| `ParkingLot *-- Floor` | Floor has no meaning outside a lot |
| `ParkingLot *-- EntryGate` | Gate belongs to the lot |
| `ParkingLot *-- ExitGate` | Gate belongs to the lot |
| `Floor *-- ParkingSpot` | Spot belongs to a floor |
| `Floor *-- DisplayBoard` | Board is part of the floor |

### Aggregation `o--`
| Relationship | Reason |
|---|---|
| `ParkingSpot o-- Vehicle` | Vehicle exists before and after parking |

### Inheritance `<\|--`
| Relationship | Reason |
|---|---|
| `ParkingSpot <\|-- MotorcycleSpot / CompactSpot / LargeSpot / EVSpot / HandicappedSpot` | Each spot type is a specialization of ParkingSpot |
| `Vehicle <\|-- Bike / Car / Truck / EVCar / HandicappedVehicle` | Each vehicle type is a specialization of Vehicle |

### Realization `<\|..`
| Relationship | Reason |
|---|---|
| `SpotAssignmentStrategy <\|.. NearestSpotStrategy / RandomSpotStrategy / FloorPreferenceStrategy` | Each strategy implements the assignment interface |
| `FeeCalculator <\|.. HourlyFeeStrategy / DailyFeeStrategy` | Each implements the fee calculation interface |
| `PaymentProcessor <\|.. CashPaymentProcessor / CardPaymentProcessor` | Each implements the payment processing interface |

### Association `-->`
| Relationship | Reason |
|---|---|
| `EntryGate --> SpotAssignmentStrategy` | Gate holds a persistent reference to its strategy |
| `ExitGate --> FeeCalculator` | Gate holds a persistent reference to fee calculator |
| `Ticket --> Vehicle` | Ticket stores vehicle reference for duration of stay |
| `Ticket --> ParkingSpot` | Ticket stores spot reference for duration of stay |
| `Payment --> Ticket` | Payment is always linked to a ticket |
| `AdminService --> ParkingLot` | Admin holds a persistent reference to the lot |

### Dependency `..`>`
| Relationship | Reason |
|---|---|
| `EntryGate ..> Ticket` | Gate creates a ticket but does not own it |
| `ExitGate ..> Payment` | Gate creates a payment but does not own it |
| `Payment ..> PaymentProcessor` | Payment calls processor once during processPayment(), does not hold it long-term |

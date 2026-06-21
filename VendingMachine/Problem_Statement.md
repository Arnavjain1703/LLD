# Vending Machine — Problem Statement

## Functional Requirements

### Product Management
- Machine holds multiple racks/shelves
- Each rack holds items of a single product type
- Each product has a name, price, and code (e.g., A1, B2)
- Rack tracks quantity available

### Item Selection
- User selects a product by code
- Machine displays price for selected item
- If item is out of stock, machine notifies user

### Payment
- Accepts coins and notes (Cash)
- Accepts card payment
- Validates that inserted amount >= item price
- Returns change if overpaid (cash only)
- Refunds full amount on cancellation

### Dispensing
- After successful payment, item is dispensed
- Rack quantity decremented
- Machine returns to idle state

### State Machine
- Idle → HasMoney → Dispensing → Idle
- User can cancel at any point before dispensing
- Machine handles: no stock, insufficient funds, exact change scenarios

### Admin Operations
- Add / remove products
- Restock items in a rack
- Collect cash from machine
- View inventory report

---

## Non-Functional Requirements
- State pattern for clean state transitions
- Strategy pattern for payment processing
- Thread-safe operations (multiple users won't interact simultaneously, but admin ops may overlap)
- Extendable payment methods
- Extendable dispensing strategies

---

## Out of Scope
- Network connectivity / remote monitoring
- Physical hardware integration
- Loyalty programs / discount codes

# Parking Lot — Problem Statement

## Functional Requirements

### Spot Management
- Spot types: Motorcycle, Compact, Large, EV, Handicapped
- Multiple floors, each floor has multiple spots of each type
- 1 vehicle per spot
- Live availability display — per floor and overall

### Vehicle Types
- Bike, Car, Truck, EV Car, Handicapped vehicle

### Spot ↔ Vehicle Compatibility
- Bike → Motorcycle spot only
- Car → Compact or Large
- Truck → Large only
- EV Car → EV spot (primary); Compact/Large as fallback if EV spots full
- Handicapped vehicle → Handicapped spots only; no fallback to regular spots

### Entry (Gate In)
- Multiple entry gates
- Vehicle arrives → gate assigns a spot → ticket issued
- Slot assignment strategy is extendable (nearest spot, random, floor-preference, etc.)

### Exit (Gate Out)
- Multiple exit gates
- Ticket scanned → fee calculated → payment → spot freed

### Pricing
- Extendable charge strategies: Hourly, Daily
- Different rates per vehicle type
- Payment methods: Cash, Card

### Admin Operations
- Add / remove floors
- Add / remove spots on a floor
- Add / remove entry and exit gates
- Update pricing strategies
- View overall and per-floor occupancy reports

---

## Non-Functional Requirements
- Extendable slot assignment strategy
- Extendable pricing strategy
- Thread-safe spot assignment (multiple gates operating concurrently)
- Walk-in only — no reservations

---

## Out of Scope
- Pre-booking / reservations
- Payment gateway integration details

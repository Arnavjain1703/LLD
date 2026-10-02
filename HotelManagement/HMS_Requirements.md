# Hotel Management System (HMS) — LLD Requirements

---

## Actors

| Actor        | Role                                                      |
|--------------|-----------------------------------------------------------|
| Guest        | Searches rooms, makes bookings, cancels, checks in/out    |
| Front Desk   | Manages walk-ins, checks in/out guests on behalf          |
| Hotel Admin  | Adds/removes rooms, marks maintenance, manages one hotel  |
| Chain Admin  | Registers hotels, manages inventory across the chain      |

---

## Functional Requirements

### 1. Hotel Management
- Chain Admin can register a new hotel with name, address, star rating
- Chain Admin can deregister a hotel
- Hotels can be searched by city and minimum star rating

### 2. Room Management
- Hotel Admin can add rooms to a hotel (room number, floor, type, capacity, price)
- Hotel Admin can mark a room as under maintenance
- Hotel Admin can restore a room from maintenance back to available
- Rooms have types: SINGLE, DOUBLE, SUITE, PENTHOUSE
- Rooms have statuses: AVAILABLE, BOOKED, OCCUPIED, UNDER_MAINTENANCE

### 3. Guest Management
- Guest can register with name, email, phone
- Guest can view their booking history
- Guest profile is shared across all hotels in the chain

### 4. Search
- Guest can search available rooms by:
  - City (required)
  - Check-in and check-out dates (required)
  - Room type (optional)
  - Minimum capacity (optional)
- Search returns hotels with their available rooms
- A room is available only if:
  - Its status is not UNDER_MAINTENANCE
  - No existing active booking overlaps the requested date range

### 5. Booking
- Guest can book one or more rooms in a single hotel
- One booking = one hotel (no cross-hotel booking in a single reservation)
- Booking requires: guest, hotel, room(s), check-in date, check-out date
- Walk-in booking allowed (no prior search required)
- Guest can view booking details at any time

### 6. Booking Lifecycle (FSM)
  PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
                ↓
           CANCELLED

- PENDING    : Booking created, payment not yet processed
- CONFIRMED  : Payment successful, awaiting guest arrival
- CHECKED_IN : Guest has arrived at the hotel
- CHECKED_OUT: Stay complete, invoice generated
- CANCELLED  : Booking cancelled by guest or hotel

- Invalid transitions must throw an exception (e.g. PENDING → CHECKED_OUT)

### 7. Check-In
- Front Desk or Guest can check in against a CONFIRMED booking
- Walk-in: Front Desk creates booking + checks in immediately
- Check-in changes booking status: CONFIRMED → CHECKED_IN
- Check-in changes room status: BOOKED → OCCUPIED

### 8. Check-Out
- Front Desk triggers check-out
- Check-out changes booking status: CHECKED_IN → CHECKED_OUT
- Check-out changes room status: OCCUPIED → AVAILABLE
- Invoice is generated automatically at check-out

### 9. Cancellation
- Guest can cancel a PENDING or CONFIRMED booking
- Cannot cancel a CHECKED_IN booking
- Refund policy:
  - Cancel 3+ days before check-in : full refund
  - Cancel 1–2 days before check-in: 50% refund
  - Cancel on the day of check-in  : no refund

### 10. Payment
- Payment is taken at booking confirmation (not at check-out)
- Supported methods: Cash, Credit/Debit Card, UPI
- On cancellation, refund is processed based on cancellation policy
- Invoice generated at check-out with line items (room nights × price + taxes)

### 11. Notifications
- Guest receives notification on:
  - Booking confirmed
  - Check-in reminder (day before)
  - Booking cancelled
  - Check-out + invoice
- Channels: Email and SMS

---

## Non-Functional Requirements

| NFR             | Requirement                                                       |
|-----------------|-------------------------------------------------------------------|
| Concurrency     | Two guests booking the same room simultaneously must be handled   |
| Consistency     | Check availability and reserve must be atomic (no TOCTOU race)   |
| Extensibility   | Dynamic pricing, loyalty points must be addable without rewrites  |
| Data Integrity  | A room cannot be double-booked for overlapping dates              |

---

## Out of Scope

| Feature               | Reason                                         |
|-----------------------|------------------------------------------------|
| Dynamic / surge pricing | Needs separate pricing engine               |
| Loyalty / rewards       | Separate service, extend via Observer later |
| Housekeeping scheduling | Operational domain, not booking domain      |
| Room service billing    | Add-on, extend Invoice later                |
| Multi-hotel booking     | Single booking = single hotel               |
| Staff shift management  | HR domain                                   |
| Reviews and ratings     | Post-stay, separate service                 |

---

## Key Constraints

- Room belongs to exactly one hotel
- Hotel does not hold a list of rooms (thin entity — RoomRepository owns rooms)
- A booking references rooms by ID, not by object embedding
- Booking overlap check: existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn
- Check-out date = next guest check-in date is valid (adjacent, not overlapping)
- Price is fixed per room per night (no dynamic pricing in scope)
- Guest ID = email address (unique, immutable)

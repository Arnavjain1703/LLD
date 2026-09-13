# Hotel Management System — Service Level Design

## The Core Question

> Hotel has `List<Room>` inside it **vs** Rooms live only in `RoomRepository` — which is better?

Two options:

| | Option A — Aggregate Root | Option B — Thin Entity (Recommended) |
|---|---|---|
| `Hotel` | holds `List<Room>` | holds **no** room list |
| Source of truth | Hotel object + RoomRepository (dual) | RoomRepository only (single) |
| Get rooms | `hotel.getRooms()` | `roomRepo.findByHotelId(id)` |
| Risk | Inconsistency if Hotel loaded without rooms | None |
| When to use | LLD interview (in-memory, simple) | Production / DDD |

---

## Option A — Hotel owns List\<Room\> (what we built)

```mermaid
classDiagram
    class Hotel {
        +String id
        +String name
        +Address address
        +double rating
        +List~Room~ rooms
        +addRoom(Room)
        +getRooms() List~Room~
    }

    class Room {
        +String roomId
        +String hotelId
        +RoomType type
        +double pricePerNight
        +RoomStatus status
    }

    class HotelRepository {
        <<interface>>
        +save(Hotel)
        +findById(String) Optional~Hotel~
        +findByCity(String) List~Hotel~
    }

    class RoomRepository {
        <<interface>>
        +save(Room)
        +findById(String) Optional~Room~
        +findByHotelId(String) List~Room~
    }

    class HotelService {
        +addRoom(hotelId, Room)
        +getRooms(hotelId) List~Room~
    }

    Hotel "1" o-- "0..*" Room : owns list
    HotelService --> HotelRepository
    HotelService --> RoomRepository
```

**Problem:** `Hotel.rooms` and `RoomRepository` are two sources of truth.  
If Hotel is reloaded from a DB later, `hotel.getRooms()` returns stale/empty data.

---

## Option B — Thin Hotel, Repository is the only source of truth ✅

```mermaid
classDiagram
    class Hotel {
        +String id
        +String name
        +Address address
        +double rating
        %% NO List~Room~ here
    }

    class Room {
        +String roomId
        +String hotelId
        +RoomType type
        +double pricePerNight
        +RoomStatus status
    }

    class HotelRepository {
        <<interface>>
        +save(Hotel)
        +findById(String) Optional~Hotel~
        +findByCity(String) List~Hotel~
    }

    class RoomRepository {
        <<interface>>
        +save(Room)
        +findById(String) Optional~Room~
        +findByHotelId(String) List~Room~
        +findByHotelIdAndType(String, RoomType) List~Room~
    }

    class HotelService {
        +addHotel(Hotel)
        +addRoom(hotelId, Room)
        +getRooms(hotelId) List~Room~
        +getRoomsByType(hotelId, RoomType) List~Room~
    }

    Room --> Hotel : hotelId (FK)
    HotelService --> HotelRepository
    HotelService --> RoomRepository
```

**Room points to Hotel via `hotelId` (like a foreign key). Hotel never holds the list.**  
`HotelService.getRooms(hotelId)` always delegates to `roomRepository.findByHotelId()`.

---

## Step-by-Step Service Flow

### addRoom()

```mermaid
sequenceDiagram
    actor Client
    participant HotelService
    participant HotelRepository
    participant RoomRepository

    Client->>HotelService: addRoom("H1", room)
    HotelService->>HotelRepository: findById("H1")
    HotelRepository-->>HotelService: Optional~Hotel~ (validates hotel exists)
    HotelService->>RoomRepository: save(room)
    RoomRepository-->>HotelService: ok
    HotelService-->>Client: void
```

### getRooms()

```mermaid
sequenceDiagram
    actor Client
    participant HotelService
    participant RoomRepository

    Client->>HotelService: getRooms("H1")
    HotelService->>RoomRepository: findByHotelId("H1")
    RoomRepository-->>HotelService: List~Room~
    HotelService-->>Client: List~Room~
```

### bookRoom() — full flow

```mermaid
sequenceDiagram
    actor Client
    participant BookingService
    participant GuestService
    participant HotelService
    participant RoomRepository
    participant BookingRepository

    Client->>BookingService: bookRoom(email, roomId, checkIn, checkOut)
    BookingService->>GuestService: getGuest(email)
    GuestService-->>BookingService: Guest ✅

    BookingService->>HotelService: getRoom(roomId)
    HotelService->>RoomRepository: findById(roomId)
    RoomRepository-->>HotelService: Room ✅
    HotelService-->>BookingService: Room

    BookingService->>BookingRepository: findActiveByRoom(roomId)
    BookingRepository-->>BookingService: List~Booking~
    BookingService->>BookingService: validateNoOverlap() ✅

    BookingService->>BookingService: pricingStrategy.calculatePrice(room, dates)
    BookingService->>BookingService: new Booking(...)
    BookingService->>RoomRepository: room.book() → status = BOOKED
    BookingService->>BookingRepository: save(booking)
    BookingService-->>Client: Booking ✅
```

### search() — filter chain

```mermaid
sequenceDiagram
    actor Client
    participant SearchService
    participant HotelRepository
    participant RoomRepository
    participant BookingRepository

    Client->>SearchService: search(criteria)
    SearchService->>HotelRepository: findAll()
    HotelRepository-->>SearchService: List~Hotel~

    SearchService->>SearchService: filter by city
    SearchService->>SearchService: filter by minRating

    loop for each matching hotel
        SearchService->>RoomRepository: findByHotelId(hotelId)
        RoomRepository-->>SearchService: List~Room~
        SearchService->>SearchService: filter by roomType
        SearchService->>BookingRepository: findActiveByRoom(roomId)
        BookingRepository-->>SearchService: active bookings
        SearchService->>SearchService: check overlaps → isAvailable()
    end

    SearchService-->>Client: List~SearchResult~
```

---

## Verdict

| Scenario | Use |
|---|---|
| LLD interview (in-memory, simple demo) | Option A — Hotel with `List<Room>` is fine |
| Production code / persistence layer | **Option B** — Repository is single source of truth |

The key principle: **Repository owns the collection, not the parent entity.**  
`Hotel` knowing about `Room` is a convenience, not a responsibility.

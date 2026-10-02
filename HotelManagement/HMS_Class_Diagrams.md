# Hotel Management System — Class Diagrams

---

## Component 1a: Hotel Management

> Responsibility: Hotel lifecycle only — register, deregister, fetch.
> Actor: Chain Admin.
> Rule: Does NOT touch rooms (that is RoomService). Does NOT query availability (that is SearchService).
>
> HotelRepository here shows only lifecycle methods.
> findByCity / findByMinStarRating are query methods — they appear only in Component 3 (Search).

```mermaid
classDiagram
    class Hotel {
        -hotelId: String
        -name: String
        -address: Address
        -starRating: int
        +getHotelId() String
        +getName() String
        +getAddress() Address
        +getStarRating() int
    }

    class Address {
        -street: String
        -city: String
        -country: String
        -zipCode: String
        +getCity() String
        +getCountry() String
    }

    class HotelRepository {
        -store: Map~String, Hotel~
        +save(Hotel) void
        +findById(String) Optional~Hotel~
        +exists(String) boolean
        +delete(String) void
        +findAll() List~Hotel~
    }

    class HotelService {
        -hotelRepository: HotelRepository
        -roomService: RoomService
        +registerHotel(Hotel) void
        +deregisterHotel(String) void
        +getHotel(String) Hotel
        +getAllHotels() List~Hotel~
    }

    Hotel            -->  Address
    HotelRepository  o--  Hotel
    HotelService     -->  HotelRepository
    HotelService     -->  RoomService
```

---

## Component 1b: Room Management

> Responsibility: Room lifecycle only — add, remove, maintenance.
> Actor: Hotel Admin.
> Rule: Does NOT register hotels (that is HotelService). Does NOT answer availability (that is SearchService).
>
> RoomRepository here shows only lifecycle methods.
> findAvailableByHotelIdAndType is a query method — it appears only in Component 3 (Search).

```mermaid
classDiagram
    class Room {
        -roomId: String
        -hotelId: String
        -roomNumber: String
        -floor: int
        -type: RoomType
        -status: RoomStatus
        -capacity: int
        -pricePerNight: BigDecimal
        +isAvailable() boolean
        +setStatus(RoomStatus) void
        +getHotelId() String
        +getType() RoomType
        +getCapacity() int
    }

    class RoomType {
        <<enumeration>>
        SINGLE
        DOUBLE
        SUITE
        PENTHOUSE
    }

    class RoomStatus {
        <<enumeration>>
        AVAILABLE
        BOOKED
        OCCUPIED
        UNDER_MAINTENANCE
    }

    class RoomRepository {
        -store: Map~String, Room~
        +save(Room) void
        +findById(String) Optional~Room~
        +findByHotelId(String) List~Room~
        +findByStatus(RoomStatus) List~Room~
        +delete(String) void
        +findAll() List~Room~
    }

    class RoomService {
        -roomRepository: RoomRepository
        -hotelRepository: HotelRepository
        +addRoom(Room) void
        +removeRoom(String) void
        +removeAllRoomsForHotel(String) void
        +getRoom(String) Room
        +getRoomsByHotel(String) List~Room~
        +markUnderMaintenance(String) void
        +markAvailable(String) void
    }

    Room            -->  RoomType
    Room            -->  RoomStatus
    RoomRepository  o--  Room
    RoomService     -->  RoomRepository
    RoomService     -->  HotelRepository
```

---

## Component 2: Guest Management

> Responsibility: Guest lifecycle — register, fetch, booking history.
> Actor: Guest (self-registration), Front Desk (walk-in).

```mermaid
classDiagram
    class Guest {
        -guestId: String
        -name: String
        -phone: String
        -bookingIds: List~String~
        +getGuestId() String
        +getName() String
        +getPhone() String
        +getBookingIds() List~String~
        +addBookingId(String) void
    }

    class GuestRepository {
        -store: Map~String, Guest~
        +save(Guest) void
        +findById(String) Optional~Guest~
        +exists(String) boolean
        +findAll() List~Guest~
    }

    class GuestService {
        -guestRepository: GuestRepository
        +registerGuest(Guest) void
        +getGuest(String) Guest
        +addBookingToGuest(String, String) void
        +getBookingHistory(String) List~String~
    }

    GuestRepository  o--  Guest
    GuestService     -->  GuestRepository
```

---

## Component 3: Search

> Responsibility: All discovery and availability queries.
> Actor: Guest.
> Rule: If the operation answers "what exists and is available?" it belongs here.
>
> Query methods from repositories are shown here — in the component that actually uses them:
>   HotelRepository.findByCity / findByMinStarRating   → used by SearchService only
>   RoomRepository.findAvailableByHotelIdAndType        → used by SearchService only
>   BookingRepository.findActiveByRoomId                → used by SearchService only

```mermaid
classDiagram
    class HotelRepository {
        +findByCity(String) List~Hotel~
        +findByMinStarRating(int) List~Hotel~
    }

    class RoomRepository {
        +findByHotelId(String) List~Room~
        +findAvailableByHotelIdAndType(String, RoomType) List~Room~
    }

    class BookingRepository {
        +findActiveByRoomId(String) List~Booking~
    }

    class SearchCriteria {
        -city: String
        -checkIn: LocalDate
        -checkOut: LocalDate
        -roomType: RoomType
        -minCapacity: int
        +getCity() String
        +getCheckIn() LocalDate
        +getCheckOut() LocalDate
        +getRoomType() RoomType
        +getMinCapacity() int
    }

    class SearchCriteriaBuilder {
        +Builder(String, LocalDate, LocalDate)
        +roomType(RoomType) Builder
        +minCapacity(int) Builder
        +build() SearchCriteria
    }

    class SearchResult {
        -hotel: Hotel
        -availableRooms: List~Room~
        +getHotel() Hotel
        +getAvailableRooms() List~Room~
        +getRoomCount() int
    }

    class SearchService {
        -hotelRepository: HotelRepository
        -roomRepository: RoomRepository
        -bookingRepository: BookingRepository
        +getHotelsByCity(String) List~Hotel~
        +getHotelsByMinStars(int) List~Hotel~
        +searchHotels(SearchCriteria) List~SearchResult~
        +searchRoomsInHotel(String, SearchCriteria) List~Room~
        +isRoomAvailable(String, LocalDate, LocalDate) boolean
    }

    SearchCriteriaBuilder  ..>  SearchCriteria : creates
    SearchResult           -->  Hotel
    SearchResult           o--  Room
    SearchService          -->  HotelRepository
    SearchService          -->  RoomRepository
    SearchService          -->  BookingRepository
```

---

## Component 4: Booking Service

> Responsibility: Full booking lifecycle — create, confirm, check-in, check-out, cancel.
> Owns the BookingStatus FSM. Handles concurrency via sorted room locking.

```mermaid
classDiagram
    class Booking {
        -bookingId: String
        -guestId: String
        -hotelId: String
        -roomIds: List~String~
        -checkIn: LocalDate
        -checkOut: LocalDate
        -status: BookingStatus
        +getBookingId() String
        +getRoomIds() List~String~
        +getStatus() BookingStatus
        +setStatus(BookingStatus) void
        +overlaps(LocalDate, LocalDate) boolean
    }

    class BookingStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        CHECKED_IN
        CHECKED_OUT
        CANCELLED
    }

    class BookingRepository {
        -store: Map~String, Booking~
        +save(Booking) void
        +findById(String) Optional~Booking~
        +findByGuestId(String) List~Booking~
        +findByHotelId(String) List~Booking~
    }

    class BookingService {
        -bookingRepository: BookingRepository
        -roomRepository: RoomRepository
        -guestRepository: GuestRepository
        -searchService: SearchService
        -paymentService: PaymentService
        -notificationService: NotificationService
        +createBooking(String, String, List~String~, LocalDate, LocalDate) Booking
        +confirmBooking(String, PaymentMethod) Booking
        +checkIn(String) Booking
        +checkOut(String) Booking
        +cancelBooking(String) Booking
    }

    Booking           -->  BookingStatus
    BookingRepository o--  Booking
    BookingService    -->  BookingRepository
    BookingService    -->  SearchService
    BookingService    -->  PaymentService
    BookingService    -->  NotificationService
```

---

## Component 5: Payment

> Responsibility: Charge, refund, invoice generation.
> Pattern: Strategy — PaymentMethod is an interface; Cash/Card/UPI are interchangeable strategies.

```mermaid
classDiagram
    class PaymentMethod {
        <<interface>>
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
        +getMethodName() String
    }

    class CashPayment {
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
        +getMethodName() String
    }

    class CardPayment {
        -cardNumber: String
        -cardHolderName: String
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
        +getMethodName() String
    }

    class UPIPayment {
        -upiId: String
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
        +getMethodName() String
    }

    class PaymentStatus {
        <<enumeration>>
        PENDING
        PAID
        REFUNDED
        FAILED
    }

    class Payment {
        -paymentId: String
        -bookingId: String
        -amount: BigDecimal
        -status: PaymentStatus
        -method: PaymentMethod
        +getPaymentId() String
        +getStatus() PaymentStatus
        +setStatus(PaymentStatus) void
        +getAmount() BigDecimal
    }

    class Invoice {
        -invoiceId: String
        -bookingId: String
        -guestName: String
        -roomCharges: BigDecimal
        -taxAmount: BigDecimal
        -totalAmount: BigDecimal
        -generatedAt: LocalDateTime
        +getTotalAmount() BigDecimal
        +print() void
    }

    class PaymentService {
        -paymentRepository: Map~String, Payment~
        +processPayment(String, BigDecimal, PaymentMethod) Payment
        +refund(String) Payment
        +generateInvoice(Booking, List~Room~) Invoice
    }

    CashPayment    ..|>  PaymentMethod
    CardPayment    ..|>  PaymentMethod
    UPIPayment     ..|>  PaymentMethod
    Payment        -->   PaymentStatus
    Payment        -->   PaymentMethod
    PaymentService -->   Payment
    PaymentService ..>   Invoice : creates
```

---

## Component 6: Notification

> Responsibility: Notify guests on booking events.
> Pattern: Observer — NotificationService holds a list of channels.
>          New channels (push, WhatsApp) plug in without changing the service.

```mermaid
classDiagram
    class NotificationChannel {
        <<interface>>
        +send(String recipient, String message) void
        +getChannelName() String
    }

    class EmailChannel {
        +send(String recipient, String message) void
        +getChannelName() String
    }

    class SMSChannel {
        +send(String recipient, String message) void
        +getChannelName() String
    }

    class NotificationService {
        -channels: List~NotificationChannel~
        +addChannel(NotificationChannel) void
        +notifyBookingConfirmed(Booking, Guest) void
        +notifyCheckIn(Booking, Guest) void
        +notifyCheckOut(Booking, Guest, Invoice) void
        +notifyCancellation(Booking, Guest) void
    }

    EmailChannel         ..|>  NotificationChannel
    SMSChannel           ..|>  NotificationChannel
    NotificationService  o--   NotificationChannel
```

---

## Component 7: HMS Facade (Singleton)

> Single entry point. Constructs and wires all repositories and services.
> Private constructor prevents external instantiation.
> Wiring order: RoomService first, then HotelService (cascade dependency).

```mermaid
classDiagram
    class HotelManagementSystem {
        -instance: HotelManagementSystem
        -hotelService: HotelService
        -roomService: RoomService
        -guestService: GuestService
        -searchService: SearchService
        -bookingService: BookingService
        -paymentService: PaymentService
        -notificationService: NotificationService
        -HotelManagementSystem()
        +getInstance() HotelManagementSystem
        +getHotelService() HotelService
        +getRoomService() RoomService
        +getGuestService() GuestService
        +getSearchService() SearchService
        +getBookingService() BookingService
        +getPaymentService() PaymentService
        +getNotificationService() NotificationService
    }

    HotelManagementSystem  -->  HotelService
    HotelManagementSystem  -->  RoomService
    HotelManagementSystem  -->  GuestService
    HotelManagementSystem  -->  SearchService
    HotelManagementSystem  -->  BookingService
    HotelManagementSystem  -->  PaymentService
    HotelManagementSystem  -->  NotificationService
```

---

## Complete System Class Diagram

```mermaid
classDiagram
    %% ── Enums ────────────────────────────────────────────────────────────────
    class RoomType {
        <<enumeration>>
        SINGLE
        DOUBLE
        SUITE
        PENTHOUSE
    }
    class RoomStatus {
        <<enumeration>>
        AVAILABLE
        BOOKED
        OCCUPIED
        UNDER_MAINTENANCE
    }
    class BookingStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        CHECKED_IN
        CHECKED_OUT
        CANCELLED
    }
    class PaymentStatus {
        <<enumeration>>
        PENDING
        PAID
        REFUNDED
        FAILED
    }

    %% ── Models ───────────────────────────────────────────────────────────────
    class Address {
        -street: String
        -city: String
        -country: String
        -zipCode: String
    }
    class Hotel {
        -hotelId: String
        -name: String
        -address: Address
        -starRating: int
    }
    class Room {
        -roomId: String
        -hotelId: String
        -floor: int
        -type: RoomType
        -status: RoomStatus
        -capacity: int
        -pricePerNight: BigDecimal
        +isAvailable() boolean
    }
    class Guest {
        -guestId: String
        -name: String
        -phone: String
        -bookingIds: List~String~
    }
    class Booking {
        -bookingId: String
        -guestId: String
        -hotelId: String
        -roomIds: List~String~
        -checkIn: LocalDate
        -checkOut: LocalDate
        -status: BookingStatus
        +overlaps(LocalDate, LocalDate) boolean
    }
    class Payment {
        -paymentId: String
        -bookingId: String
        -amount: BigDecimal
        -status: PaymentStatus
        -method: PaymentMethod
    }
    class Invoice {
        -invoiceId: String
        -bookingId: String
        -roomCharges: BigDecimal
        -taxAmount: BigDecimal
        -totalAmount: BigDecimal
    }
    class SearchCriteria {
        -city: String
        -checkIn: LocalDate
        -checkOut: LocalDate
        -roomType: RoomType
        -minCapacity: int
    }
    class SearchResult {
        -hotel: Hotel
        -availableRooms: List~Room~
    }

    %% ── Interfaces ───────────────────────────────────────────────────────────
    class PaymentMethod {
        <<interface>>
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
    }
    class NotificationChannel {
        <<interface>>
        +send(String, String) void
    }

    %% ── Implementations ──────────────────────────────────────────────────────
    class CashPayment { +pay(BigDecimal) boolean }
    class CardPayment { -cardNumber: String }
    class UPIPayment  { -upiId: String }
    class EmailChannel { +send(String, String) void }
    class SMSChannel   { +send(String, String) void }

    %% ── Repositories (all methods for complete picture) ─────────────────────
    class HotelRepository {
        +save(Hotel) void
        +findById(String) Optional~Hotel~
        +exists(String) boolean
        +delete(String) void
        +findAll() List~Hotel~
        +findByCity(String) List~Hotel~
        +findByMinStarRating(int) List~Hotel~
    }
    class RoomRepository {
        +save(Room) void
        +findById(String) Optional~Room~
        +findByHotelId(String) List~Room~
        +findByStatus(RoomStatus) List~Room~
        +delete(String) void
        +findAvailableByHotelIdAndType(String, RoomType) List~Room~
    }
    class GuestRepository {
        +save(Guest) void
        +findById(String) Optional~Guest~
    }
    class BookingRepository {
        +save(Booking) void
        +findById(String) Optional~Booking~
        +findActiveByRoomId(String) List~Booking~
        +findByGuestId(String) List~Booking~
    }

    %% ── Services ─────────────────────────────────────────────────────────────
    class HotelService {
        +registerHotel(Hotel) void
        +deregisterHotel(String) void
        +getHotel(String) Hotel
        +getAllHotels() List~Hotel~
    }
    class RoomService {
        +addRoom(Room) void
        +removeRoom(String) void
        +removeAllRoomsForHotel(String) void
        +getRoom(String) Room
        +getRoomsByHotel(String) List~Room~
        +markUnderMaintenance(String) void
        +markAvailable(String) void
    }
    class GuestService {
        +registerGuest(Guest) void
        +getGuest(String) Guest
        +addBookingToGuest(String, String) void
    }
    class SearchService {
        +getHotelsByCity(String) List~Hotel~
        +getHotelsByMinStars(int) List~Hotel~
        +searchHotels(SearchCriteria) List~SearchResult~
        +searchRoomsInHotel(String, SearchCriteria) List~Room~
        +isRoomAvailable(String, LocalDate, LocalDate) boolean
    }
    class BookingService {
        +createBooking(String, String, List~String~, LocalDate, LocalDate) Booking
        +confirmBooking(String, PaymentMethod) Booking
        +checkIn(String) Booking
        +checkOut(String) Booking
        +cancelBooking(String) Booking
    }
    class PaymentService {
        +processPayment(String, BigDecimal, PaymentMethod) Payment
        +refund(String) Payment
        +generateInvoice(Booking) Invoice
    }
    class NotificationService {
        +notifyBookingConfirmed(Booking, Guest) void
        +notifyCheckIn(Booking, Guest) void
        +notifyCheckOut(Booking, Guest, Invoice) void
        +notifyCancellation(Booking, Guest) void
    }

    %% ── Facade ───────────────────────────────────────────────────────────────
    class HotelManagementSystem {
        -instance: HotelManagementSystem
        +getInstance() HotelManagementSystem
    }

    %% ── Model Relationships ──────────────────────────────────────────────────
    Hotel        -->  Address
    Room         -->  RoomType
    Room         -->  RoomStatus
    Booking      -->  BookingStatus
    Payment      -->  PaymentStatus
    Payment      -->  PaymentMethod
    SearchResult -->  Hotel
    SearchResult o--  Room

    %% ── Interface Implementations ────────────────────────────────────────────
    CashPayment  ..|>  PaymentMethod
    CardPayment  ..|>  PaymentMethod
    UPIPayment   ..|>  PaymentMethod
    EmailChannel ..|>  NotificationChannel
    SMSChannel   ..|>  NotificationChannel

    %% ── Repository Ownership ─────────────────────────────────────────────────
    HotelRepository   o--  Hotel
    RoomRepository    o--  Room
    GuestRepository   o--  Guest
    BookingRepository o--  Booking

    %% ── Service Dependencies ─────────────────────────────────────────────────
    HotelService   -->  HotelRepository
    HotelService   -->  RoomService
    RoomService    -->  RoomRepository
    RoomService    -->  HotelRepository
    GuestService   -->  GuestRepository
    SearchService  -->  HotelRepository
    SearchService  -->  RoomRepository
    SearchService  -->  BookingRepository
    BookingService -->  BookingRepository
    BookingService -->  RoomRepository
    BookingService -->  GuestRepository
    BookingService -->  SearchService
    BookingService -->  PaymentService
    BookingService -->  NotificationService
    PaymentService ..>  Invoice : creates
    NotificationService o-- NotificationChannel

    %% ── Facade ───────────────────────────────────────────────────────────────
    HotelManagementSystem  -->  HotelService
    HotelManagementSystem  -->  RoomService
    HotelManagementSystem  -->  GuestService
    HotelManagementSystem  -->  SearchService
    HotelManagementSystem  -->  BookingService
    HotelManagementSystem  -->  PaymentService
    HotelManagementSystem  -->  NotificationService
```

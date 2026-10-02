# Hotel Management System — Class Diagrams

---

## Component 1: Hotel & Room Management

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

    class HotelRepository {
        -store: Map~String, Hotel~
        +save(Hotel) void
        +findById(String) Optional~Hotel~
        +findByCity(String) List~Hotel~
        +findByMinStarRating(int) List~Hotel~
        +exists(String) boolean
        +findAll() List~Hotel~
    }

    class RoomRepository {
        -store: Map~String, Room~
        +save(Room) void
        +findById(String) Optional~Room~
        +findByHotelId(String) List~Room~
        +findAvailableByHotelId(String) List~Room~
        +findAvailableByHotelIdAndType(String, RoomType) List~Room~
        +findByStatus(RoomStatus) List~Room~
    }

    class HotelService {
        -hotelRepository: HotelRepository
        -roomRepository: RoomRepository
        +registerHotel(Hotel) void
        +getHotel(String) Hotel
        +getHotelsByCity(String) List~Hotel~
        +getHotelsByMinStars(int) List~Hotel~
        +addRoom(Room) void
        +getRoom(String) Room
        +getRoomsByHotel(String) List~Room~
        +getAvailableRooms(String, RoomType) List~Room~
        +markUnderMaintenance(String) void
        +markAvailable(String) void
    }

    Hotel       "1" --> "1"  Address
    Room        "1" --> "1"  RoomType
    Room        "1" --> "1"  RoomStatus
    HotelService --> HotelRepository
    HotelService --> RoomRepository
    HotelRepository o-- Hotel
    RoomRepository  o-- Room
```

---

## Component 2: Guest Management

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
        +findAll() List~Guest~
        +exists(String) boolean
    }

    class GuestService {
        -guestRepository: GuestRepository
        +registerGuest(Guest) void
        +getGuest(String) Guest
        +addBookingToGuest(String, String) void
        +getBookingHistory(String) List~String~
    }

    GuestService    --> GuestRepository
    GuestRepository o-- Guest
```

---

## Component 3: Search

```mermaid
classDiagram
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
        -city: String
        -checkIn: LocalDate
        -checkOut: LocalDate
        -roomType: RoomType
        -minCapacity: int
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
        +searchHotels(SearchCriteria) List~SearchResult~
        +searchRoomsInHotel(String, SearchCriteria) List~Room~
        +isRoomAvailable(String, LocalDate, LocalDate) boolean
    }

    SearchCriteriaBuilder ..> SearchCriteria : creates
    SearchResult --> Hotel
    SearchResult o-- Room
    SearchService --> HotelRepository
    SearchService --> RoomRepository
    SearchService --> BookingRepository
```

---

## Component 4: Booking Service

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
        +getGuestId() String
        +getHotelId() String
        +getRoomIds() List~String~
        +getCheckIn() LocalDate
        +getCheckOut() LocalDate
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
        +findActiveByRoomId(String) List~Booking~
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

    Booking       "1" --> "1"  BookingStatus
    BookingRepository  o-- Booking
    BookingService --> BookingRepository
    BookingService --> SearchService
    BookingService --> PaymentService
    BookingService --> NotificationService
```

---

## Component 5: Payment

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
        +getInvoiceId() String
        +getTotalAmount() BigDecimal
        +print() void
    }

    class PaymentService {
        -paymentRepository: Map~String, Payment~
        +processPayment(String, BigDecimal, PaymentMethod) Payment
        +refund(String) Payment
        +generateInvoice(Booking, Room) Invoice
    }

    CashPayment ..|> PaymentMethod
    CardPayment ..|> PaymentMethod
    UPIPayment  ..|> PaymentMethod
    Payment     "1" --> "1" PaymentStatus
    Payment     "1" --> "1" PaymentMethod
    PaymentService --> Payment
    PaymentService ..> Invoice : creates
```

---

## Component 6: Notification

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

    EmailChannel ..|> NotificationChannel
    SMSChannel   ..|> NotificationChannel
    NotificationService o-- NotificationChannel
```

---

## Component 7: HMS Facade (Singleton)

```mermaid
classDiagram
    class HotelManagementSystem {
        -instance: HotelManagementSystem
        -hotelService: HotelService
        -guestService: GuestService
        -searchService: SearchService
        -bookingService: BookingService
        -paymentService: PaymentService
        -notificationService: NotificationService
        -HotelManagementSystem()
        +getInstance() HotelManagementSystem
        +getHotelService() HotelService
        +getGuestService() GuestService
        +getSearchService() SearchService
        +getBookingService() BookingService
        +getPaymentService() PaymentService
        +getNotificationService() NotificationService
    }

    HotelManagementSystem --> HotelService
    HotelManagementSystem --> GuestService
    HotelManagementSystem --> SearchService
    HotelManagementSystem --> BookingService
    HotelManagementSystem --> PaymentService
    HotelManagementSystem --> NotificationService
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
        -roomNumber: String
        -floor: int
        -type: RoomType
        -status: RoomStatus
        -capacity: int
        -pricePerNight: BigDecimal
        +isAvailable() boolean
        +overlaps via Booking
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

    %% ── Payment Strategies ───────────────────────────────────────────────────
    class CashPayment {
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
    }
    class CardPayment {
        -cardNumber: String
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
    }
    class UPIPayment {
        -upiId: String
        +pay(BigDecimal) boolean
        +refund(BigDecimal) boolean
    }

    %% ── Notification Channels ────────────────────────────────────────────────
    class EmailChannel {
        +send(String, String) void
    }
    class SMSChannel {
        +send(String, String) void
    }

    %% ── Repositories ─────────────────────────────────────────────────────────
    class HotelRepository {
        +save(Hotel) void
        +findById(String) Optional~Hotel~
        +findByCity(String) List~Hotel~
    }
    class RoomRepository {
        +save(Room) void
        +findById(String) Optional~Room~
        +findByHotelId(String) List~Room~
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
        +addRoom(Room) void
        +markUnderMaintenance(String) void
        +markAvailable(String) void
    }
    class GuestService {
        +registerGuest(Guest) void
        +getGuest(String) Guest
        +addBookingToGuest(String, String) void
    }
    class SearchService {
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
        +getHotelService() HotelService
        +getGuestService() GuestService
        +getSearchService() SearchService
        +getBookingService() BookingService
        +getPaymentService() PaymentService
        +getNotificationService() NotificationService
    }

    %% ── Relationships ────────────────────────────────────────────────────────
    Hotel       --> Address
    Room        --> RoomType
    Room        --> RoomStatus
    Booking     --> BookingStatus
    Payment     --> PaymentStatus
    Payment     --> PaymentMethod
    SearchResult --> Hotel
    SearchResult o-- Room

    CashPayment ..|> PaymentMethod
    CardPayment ..|> PaymentMethod
    UPIPayment  ..|> PaymentMethod
    EmailChannel ..|> NotificationChannel
    SMSChannel   ..|> NotificationChannel

    HotelRepository  o-- Hotel
    RoomRepository   o-- Room
    GuestRepository  o-- Guest
    BookingRepository o-- Booking

    HotelService --> HotelRepository
    HotelService --> RoomRepository
    GuestService --> GuestRepository
    SearchService --> HotelRepository
    SearchService --> RoomRepository
    SearchService --> BookingRepository
    BookingService --> BookingRepository
    BookingService --> RoomRepository
    BookingService --> GuestRepository
    BookingService --> SearchService
    BookingService --> PaymentService
    BookingService --> NotificationService
    PaymentService ..> Invoice : creates

    NotificationService o-- NotificationChannel

    HotelManagementSystem --> HotelService
    HotelManagementSystem --> GuestService
    HotelManagementSystem --> SearchService
    HotelManagementSystem --> BookingService
    HotelManagementSystem --> PaymentService
    HotelManagementSystem --> NotificationService
```

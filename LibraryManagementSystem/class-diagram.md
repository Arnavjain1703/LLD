# Library Management System — Class Diagrams

---

## Step 1 — Core Domain

```mermaid
classDiagram
    class BookFormat {
        <<enumeration>>
        PHYSICAL
        EBOOK
        JOURNAL
        AUDIO_BOOK
    }

    class BookItemStatus {
        <<enumeration>>
        AVAILABLE
        BORROWED
        RESERVED
        LOST
        DAMAGED
    }

    class LibraryItem {
        <<abstract>>
        +String isbn
        +String title
        +List~String~ authors
        +String genre
        +String publisher
        +String language
        +String edition
        +BookFormat format
    }

    class Book {
        +List~BookItem~ copies
        +getAvailableCopy() BookItem
    }

    class EBook {
        +String downloadUrl
        +int maxConcurrentBorrows
        +int activeBorrows
    }

    class Journal {
        +String volume
        +String issueNumber
        +LocalDate publishDate
    }

    class AudioBook {
        +String narratorName
        +int durationMinutes
    }

    class BookItem {
        +String barcode
        +BookItemStatus status
        +RackLocation rack
        +synchronized checkout(Member) BookLending
        +synchronized markReturned()
        +markLost()
        +markDamaged()
    }

    class RackLocation {
        +String branchId
        +String aisle
        +String shelf
        +String position
    }

    LibraryItem <|-- Book
    LibraryItem <|-- EBook
    LibraryItem <|-- Journal
    LibraryItem <|-- AudioBook
    LibraryItem --> BookFormat
    Book "1" *-- "1..*" BookItem : has copies
    BookItem "1" *-- "1" RackLocation : located at
    BookItem --> BookItemStatus
```

---

## Step 2 — People

```mermaid
classDiagram
    class MemberStatus {
        <<enumeration>>
        ACTIVE
        SUSPENDED
        BLACKLISTED
    }

    class MemberTier {
        <<enumeration>>
        REGULAR
        PREMIUM
    }

    class Person {
        <<abstract>>
        +String id
        +String name
        +String email
        +String phone
        +Address address
    }

    class Member {
        +MemberStatus status
        +MemberTier tier
        +int MAX_BORROW_LIMIT = 5
        +canBorrow() boolean
        +hasPendingFine() boolean
    }

    class Librarian {
        +String employeeId
        +addBook(Book)
        +removeBook(String isbn)
        +updateBook(Book)
        +suspendMember(String memberId)
        +reactivateMember(String memberId)
        +blacklistMember(String memberId)
        +waiveFine(Fine)
    }

    Person <|-- Member
    Person <|-- Librarian
    Member --> MemberStatus
    Member --> MemberTier
```

---

## Step 3 — Transactions

```mermaid
classDiagram
    class ReservationStatus {
        <<enumeration>>
        WAITING
        NOTIFIED
        COMPLETED
        CANCELLED
        EXPIRED
    }

    class BookLending {
        +String lendingId
        +BookItem bookItem
        +Member member
        +LocalDate issueDate
        +LocalDate dueDate
        +LocalDate returnDate
        +boolean isOverdue()
        +long overdueDays()
    }

    class BookReservation {
        +String reservationId
        +Book book
        +Member member
        +LocalDateTime reservationDate
        +LocalDateTime notifiedAt
        +ReservationStatus status
        +cancel()
        +expire()
    }

    class Fine {
        +String fineId
        +BookLending lending
        +Member member
        +double amount
        +boolean waived
        +boolean paid
        +LocalDate createdAt
        +pay()
        +waive()
    }

    class LendingAuditLog {
        +String eventId
        +String lendingId
        +String memberId
        +String bookItemBarcode
        +String eventType
        +LocalDateTime timestamp
        +String performedBy
    }

    BookLending "1" --> "1" BookItem
    BookLending "1" --> "1" Member
    BookLending --> LendingAuditLog : creates
    BookReservation "1" --> "1" Book
    BookReservation "1" --> "1" Member
    BookReservation --> ReservationStatus
    Fine "1" --> "1" BookLending
    Fine "1" --> "1" Member
    Member "1" o-- "0..*" BookLending : active lendings
    Member "1" o-- "0..*" BookReservation : reservations
    Member "1" o-- "0..1" Fine : outstanding fine
```

---

## Step 4 — Fine Strategy

```mermaid
classDiagram
    class FineStrategy {
        <<interface>>
        +calculate(BookLending) double
    }

    class RegularFineStrategy {
        +double RATE_PER_DAY = 1.0
        +calculate(BookLending) double
    }

    class PremiumFineStrategy {
        +double RATE_PER_DAY = 0.5
        +calculate(BookLending) double
    }

    class WaivedFineStrategy {
        +calculate(BookLending) double
    }

    class FineStrategyFactory {
        +getStrategy(MemberTier) FineStrategy
    }

    FineStrategy <|.. RegularFineStrategy
    FineStrategy <|.. PremiumFineStrategy
    FineStrategy <|.. WaivedFineStrategy
    FineStrategyFactory --> FineStrategy : creates
    FineStrategyFactory --> MemberTier
```

---

## Step 5 — Notifications

```mermaid
classDiagram
    class NotificationChannel {
        <<enumeration>>
        EMAIL
        SMS
    }

    class NotificationService {
        <<interface>>
        +notify(Member, String message, NotificationChannel)
    }

    class EmailNotificationService {
        +notify(Member, String message, NotificationChannel)
    }

    class SMSNotificationService {
        +notify(Member, String message, NotificationChannel)
    }

    class NotificationDispatcher {
        +Map~NotificationChannel, NotificationService~ handlers
        +dispatch(Member, String message)
    }

    NotificationService <|.. EmailNotificationService
    NotificationService <|.. SMSNotificationService
    NotificationDispatcher --> NotificationService
    NotificationDispatcher --> NotificationChannel
```

---

## Step 6 — Search & Catalog

```mermaid
classDiagram
    class SearchCriteria {
        +String title
        +String author
        +String isbn
        +String genre
        +String subject
        +BookFormat format
        +boolean availableOnly
        +int page
        +int pageSize
    }

    class SearchService {
        <<interface>>
        +search(SearchCriteria) List~LibraryItem~
    }

    class CatalogSearchService {
        +Map~String, List~Book~~ byTitle
        +Map~String, List~Book~~ byAuthor
        +Map~String, Book~ byISBN
        +Map~String, List~Book~~ byGenre
        +search(SearchCriteria) List~LibraryItem~
        +indexBook(Book)
        +removeBook(String isbn)
    }

    SearchService <|.. CatalogSearchService
    CatalogSearchService --> SearchCriteria
    CatalogSearchService --> Book
```

---

## Step 7 — Services

```mermaid
classDiagram
    class BorrowService {
        -SearchService searchService
        -NotificationDispatcher notificationDispatcher
        -FineStrategyFactory fineStrategyFactory
        -BookLendingRepository lendingRepo
        +borrowBook(Member, String barcode) BookLending
        +validateMember(Member)
        -selectAvailableCopy(Book) BookItem
    }

    class ReturnService {
        -BookLendingRepository lendingRepo
        -ReservationQueue reservationQueue
        -FineStrategyFactory fineStrategyFactory
        -NotificationDispatcher notificationDispatcher
        +returnBook(Member, String barcode) Fine
        -triggerReservationNotification(Book)
    }

    class ReservationService {
        -ReservationQueue reservationQueue
        -NotificationDispatcher notificationDispatcher
        +reserve(Member, Book) BookReservation
        +cancel(String reservationId)
        +expireStale()
        +notifyNext(Book)
    }

    class FineService {
        -FineRepository fineRepo
        -FineStrategyFactory fineStrategyFactory
        +calculateFine(BookLending) Fine
        +payFine(String fineId)
        +waiveFine(String fineId, Librarian)
    }

    class MemberService {
        -MemberRepository memberRepo
        +register(Member)
        +suspend(String memberId)
        +reactivate(String memberId)
        +blacklist(String memberId)
        +getBorrowHistory(String memberId) List~BookLending~
        +getFineHistory(String memberId) List~Fine~
    }

    BorrowService --> SearchService
    BorrowService --> NotificationDispatcher
    BorrowService --> FineStrategyFactory
    BorrowService --> BookLendingRepository
    ReturnService --> BookLendingRepository
    ReturnService --> ReservationQueue
    ReturnService --> FineStrategyFactory
    ReturnService --> NotificationDispatcher
    ReservationService --> ReservationQueue
    ReservationService --> NotificationDispatcher
    FineService --> FineRepository
    FineService --> FineStrategyFactory
    MemberService --> MemberRepository
```

---

## Step 8 — Infrastructure

```mermaid
classDiagram
    class BookLendingRepository {
        <<interface>>
        +save(BookLending)
        +findById(String) BookLending
        +findActiveByMember(String memberId) List~BookLending~
        +findByBookItem(String barcode) BookLending
    }

    class FineRepository {
        <<interface>>
        +save(Fine)
        +findByMember(String memberId) List~Fine~
        +findUnpaidByMember(String memberId) List~Fine~
    }

    class MemberRepository {
        <<interface>>
        +save(Member)
        +findById(String) Member
        +findByEmail(String) Member
    }

    class ReservationQueue {
        +Map~String, Queue~BookReservation~~ queues
        +enqueue(BookReservation)
        +dequeue(String isbn) BookReservation
        +peek(String isbn) BookReservation
        +remove(String reservationId)
    }

    class Branch {
        +String branchId
        +String name
        +Address address
        +CatalogSearchService catalog
        +List~Librarian~ staff
    }

    class Library {
        <<singleton>>
        -static Library instance
        +List~Branch~ branches
        +MemberService memberService
        +BorrowService borrowService
        +ReturnService returnService
        +ReservationService reservationService
        +FineService fineService
        +static getInstance() Library
    }

    Library "1" *-- "1..*" Branch
    Library --> BorrowService
    Library --> ReturnService
    Library --> ReservationService
    Library --> FineService
    Library --> MemberService
    Branch "1" *-- "1" CatalogSearchService
    Branch "1" o-- "0..*" Librarian
    ReservationQueue "1" --> "0..*" BookReservation
```

---

## Step 9 — Full Combined Diagram

```mermaid
classDiagram
    class BookFormat {
        <<enumeration>>
        PHYSICAL
        EBOOK
        JOURNAL
        AUDIO_BOOK
    }
    class BookItemStatus {
        <<enumeration>>
        AVAILABLE
        BORROWED
        RESERVED
        LOST
        DAMAGED
    }
    class MemberStatus {
        <<enumeration>>
        ACTIVE
        SUSPENDED
        BLACKLISTED
    }
    class MemberTier {
        <<enumeration>>
        REGULAR
        PREMIUM
    }
    class ReservationStatus {
        <<enumeration>>
        WAITING
        NOTIFIED
        COMPLETED
        CANCELLED
        EXPIRED
    }
    class NotificationChannel {
        <<enumeration>>
        EMAIL
        SMS
    }
    class LibraryItem {
        <<abstract>>
        +String isbn
        +String title
        +List~String~ authors
        +String genre
        +String publisher
        +String language
        +BookFormat format
    }
    class Book {
        +List~BookItem~ copies
        +getAvailableCopy() BookItem
    }
    class EBook {
        +String downloadUrl
        +int maxConcurrentBorrows
        +int activeBorrows
    }
    class Journal {
        +String volume
        +String issueNumber
        +LocalDate publishDate
    }
    class AudioBook {
        +String narratorName
        +int durationMinutes
    }
    class BookItem {
        +String barcode
        +BookItemStatus status
        +synchronized checkout(Member) BookLending
        +synchronized markReturned()
        +markLost()
        +markDamaged()
    }
    class RackLocation {
        +String branchId
        +String aisle
        +String shelf
        +String position
    }
    class Person {
        <<abstract>>
        +String id
        +String name
        +String email
        +String phone
    }
    class Member {
        +MemberStatus status
        +MemberTier tier
        +int MAX_BORROW_LIMIT = 5
        +canBorrow() boolean
        +hasPendingFine() boolean
    }
    class Librarian {
        +String employeeId
        +addBook(Book)
        +removeBook(String isbn)
        +suspendMember(String memberId)
        +waiveFine(Fine)
    }
    class BookLending {
        +String lendingId
        +LocalDate issueDate
        +LocalDate dueDate
        +LocalDate returnDate
        +boolean isOverdue()
        +long overdueDays()
    }
    class BookReservation {
        +String reservationId
        +LocalDateTime reservationDate
        +LocalDateTime notifiedAt
        +ReservationStatus status
        +cancel()
        +expire()
    }
    class Fine {
        +String fineId
        +double amount
        +boolean waived
        +boolean paid
        +pay()
        +waive()
    }
    class LendingAuditLog {
        +String eventId
        +String eventType
        +LocalDateTime timestamp
        +String performedBy
    }
    class FineStrategy {
        <<interface>>
        +calculate(BookLending) double
    }
    class RegularFineStrategy {
        +double RATE_PER_DAY = 1.0
        +calculate(BookLending) double
    }
    class PremiumFineStrategy {
        +double RATE_PER_DAY = 0.5
        +calculate(BookLending) double
    }
    class WaivedFineStrategy {
        +calculate(BookLending) double
    }
    class FineStrategyFactory {
        +getStrategy(MemberTier) FineStrategy
    }
    class NotificationService {
        <<interface>>
        +notify(Member, String, NotificationChannel)
    }
    class EmailNotificationService {
        +notify(Member, String, NotificationChannel)
    }
    class SMSNotificationService {
        +notify(Member, String, NotificationChannel)
    }
    class NotificationDispatcher {
        +Map~NotificationChannel, NotificationService~ handlers
        +dispatch(Member, String message)
    }
    class SearchCriteria {
        +String title
        +String author
        +String isbn
        +String genre
        +boolean availableOnly
        +int page
        +int pageSize
    }
    class SearchService {
        <<interface>>
        +search(SearchCriteria) List~LibraryItem~
    }
    class CatalogSearchService {
        +Map~String, List~Book~~ byTitle
        +Map~String, Book~ byISBN
        +Map~String, List~Book~~ byGenre
        +search(SearchCriteria) List~LibraryItem~
        +indexBook(Book)
        +removeBook(String isbn)
    }
    class BorrowService {
        +borrowBook(Member, String barcode) BookLending
        +validateMember(Member)
    }
    class ReturnService {
        +returnBook(Member, String barcode) Fine
    }
    class ReservationService {
        +reserve(Member, Book) BookReservation
        +cancel(String reservationId)
        +expireStale()
        +notifyNext(Book)
    }
    class FineService {
        +calculateFine(BookLending) Fine
        +payFine(String fineId)
        +waiveFine(String fineId, Librarian)
    }
    class MemberService {
        +register(Member)
        +suspend(String memberId)
        +reactivate(String memberId)
        +blacklist(String memberId)
    }
    class BookLendingRepository {
        <<interface>>
        +save(BookLending)
        +findById(String) BookLending
        +findActiveByMember(String) List~BookLending~
    }
    class FineRepository {
        <<interface>>
        +save(Fine)
        +findUnpaidByMember(String) List~Fine~
    }
    class MemberRepository {
        <<interface>>
        +save(Member)
        +findById(String) Member
    }
    class ReservationQueue {
        +enqueue(BookReservation)
        +dequeue(String isbn) BookReservation
        +remove(String reservationId)
    }
    class Branch {
        +String branchId
        +String name
        +CatalogSearchService catalog
    }
    class Library {
        <<singleton>>
        -static Library instance
        +List~Branch~ branches
        +static getInstance() Library
    }

    LibraryItem <|-- Book
    LibraryItem <|-- EBook
    LibraryItem <|-- Journal
    LibraryItem <|-- AudioBook
    LibraryItem --> BookFormat
    Book "1" *-- "1..*" BookItem
    BookItem "1" *-- "1" RackLocation
    BookItem --> BookItemStatus
    Person <|-- Member
    Person <|-- Librarian
    Member --> MemberStatus
    Member --> MemberTier
    Member "1" o-- "0..*" BookLending
    Member "1" o-- "0..*" BookReservation
    Member "1" o-- "0..1" Fine
    BookLending "1" --> "1" BookItem
    BookLending "1" --> "1" Member
    BookLending --> LendingAuditLog : creates
    BookReservation "1" --> "1" Book
    BookReservation "1" --> "1" Member
    BookReservation --> ReservationStatus
    Fine "1" --> "1" BookLending
    Fine "1" --> "1" Member
    FineStrategy <|.. RegularFineStrategy
    FineStrategy <|.. PremiumFineStrategy
    FineStrategy <|.. WaivedFineStrategy
    FineStrategyFactory --> FineStrategy
    FineStrategyFactory --> MemberTier
    NotificationService <|.. EmailNotificationService
    NotificationService <|.. SMSNotificationService
    NotificationDispatcher --> NotificationService
    NotificationDispatcher --> NotificationChannel
    SearchService <|.. CatalogSearchService
    BorrowService --> SearchService
    BorrowService --> NotificationDispatcher
    BorrowService --> FineStrategyFactory
    BorrowService --> BookLendingRepository
    ReturnService --> BookLendingRepository
    ReturnService --> ReservationQueue
    ReturnService --> FineStrategyFactory
    ReturnService --> NotificationDispatcher
    ReservationService --> ReservationQueue
    ReservationService --> NotificationDispatcher
    FineService --> FineRepository
    FineService --> FineStrategyFactory
    MemberService --> MemberRepository
    Library "1" *-- "1..*" Branch
    Library --> BorrowService
    Library --> ReturnService
    Library --> ReservationService
    Library --> FineService
    Library --> MemberService
    Branch "1" *-- "1" CatalogSearchService
    Branch "1" o-- "0..*" Librarian
    ReservationQueue "1" --> "0..*" BookReservation
```

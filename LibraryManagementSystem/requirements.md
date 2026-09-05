# Library Management System — SDE3 Requirements

## Scope
Physical books only. Each book title can have one or more physical copies (`BookItem`).  
Book catalog is managed via `BookService` backed by a `BookRepository`.

---

## Actors
- **Member** — searches, borrows, returns, reserves books, pays fines
- **Librarian** — adds/removes books and copies, manages members, waives fines
- **System** — auto-notifies on reservation availability, auto-expires reservations, auto-calculates fines

---

## Functional Requirements

### Book Management (via `BookService`)
- Add a new book to the catalog (stored in `BookRepository`)
- Add a new physical copy (`BookItem`) to an existing book
- Delete a book from the catalog (and all its copies)
- Delete a specific physical copy (`BookItem`) by barcode
- Fetch a book by ISBN
- Fetch all books in the catalog
- Search books (delegates to `CatalogSearchService`)
- `BookRepository` keeps the source of truth; `CatalogSearchService` is kept in sync on every add/delete

### Search & Catalog
- Search by title, author, ISBN, genre
- Filter by availability (copies currently available to borrow)
- Paginated results

### Borrowing
- Member can borrow up to 5 books at a time
- Default loan period: 14 days
- Cannot borrow if account is suspended or blacklisted
- Cannot borrow if no available copy exists → offer reservation instead
- A member cannot borrow the same title twice simultaneously

### Returning
- Record return date against the `BookLending` record
- Calculate and apply fine if overdue
- Mark `BookItem` status back to `AVAILABLE`
- Trigger reservation notification if other members are waiting

### Reservations
- Member can reserve a book that has no available copy
- Reservation queue is FIFO per book title
- When a copy is returned, the first waiting member is notified
- Reservation expires if not acted upon within 3 days of notification
- On expiry, next member in queue is automatically notified
- Member can cancel a reservation at any time

### Fines
- Charged per overdue day at a configurable rate
- Fine rate varies by member tier: Regular (₹1/day), Premium (₹0.5/day)
- Member must clear outstanding fine before borrowing further
- Librarian can waive a fine
- Fine payment is independent of the return — member can return the book and still owe a fine

### Notifications
- Notify member when their reserved book becomes available
- Notify member 2 days before due date as a reminder
- Notification channels: Email, SMS (pluggable — new channels must not require service changes)

### Member Management
- Register, suspend, reactivate, blacklist members
- View full borrowing history
- View full fine history

---

## Non-Functional Requirements

- **Concurrency**: simultaneous checkout of the last copy must be safe — no double-borrow (`synchronized` on `BookItem`)
- **Consistency**: reservation queue notifications must serialize — two members must not both receive the same copy
- **Auditability**: full immutable history of all borrow/return/fine events via `LendingAuditLog`
- **Extensibility**: new fine policies must not require changes to `BorrowService` or `ReturnService` (Strategy pattern)
- **Extensibility**: new notification channels must not require changes to any service (pluggable `NotificationDispatcher`)
- **Availability**: catalog search (`BookService.search`) should remain read-only and available even if borrow/return services are down

---

## Constraints & Edge Cases

- Lost book: `BookItem` marked `LOST`, removed from circulation, member charged replacement cost
- Damaged book: `BookItem` marked `DAMAGED`, removed from circulation, librarian decides replacement charge
- Deleting a book that has active lendings must be blocked or flagged — cannot remove a copy currently borrowed
- Reservation queue must handle expiry and cascade to next member automatically
- `BookRepository` is the single source of truth for all book/copy data — `CatalogSearchService` is a derived read index

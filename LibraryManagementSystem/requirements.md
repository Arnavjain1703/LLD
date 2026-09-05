# Library Management System — SDE3 Requirements

## Actors
- **Member** — searches, borrows, returns, reserves books, pays fines
- **Librarian** — adds/removes books, manages members, waives fines
- **System** — auto-notifies on reservation availability, auto-expires reservations, auto-calculates fines

---

## Functional Requirements

### Book Management
- Add, update, remove books from the catalog
- Each book title can have multiple physical copies (`BookItem`)
- Support for multiple formats: physical book, e-book, journal, audio book
- Books have: ISBN, title, author(s), genre, publisher, language, edition

### Search & Catalog
- Search by title, author, ISBN, genre, subject
- Filter by availability
- Paginated results

### Borrowing
- Member can borrow up to 5 books at a time
- Default loan period: 14 days
- Cannot borrow if account is suspended or blacklisted
- Cannot borrow if no available copy exists → offer reservation instead

### Returning
- Record return date
- Calculate and apply fine if overdue
- Mark copy as available
- Trigger reservation notification if copies were reserved

### Reservations
- Member can reserve a book with no available copy
- Reservation queue is FIFO per book title
- When a copy is returned, the first waiting member is notified
- Reservation expires if not acted upon within 3 days of notification
- Member can cancel a reservation at any time

### Fines
- Charged per overdue day (configurable rate)
- Fine policy can vary by member tier (regular, premium)
- Fine must be paid before borrowing further
- Librarian can waive a fine

### Notifications
- Notify member when reserved book becomes available
- Notify member 2 days before due date
- Support multiple channels: email, SMS (pluggable)

### Member Management
- Register, suspend, reactivate, blacklist members
- View full borrowing and fine history

---

## Non-Functional Requirements

- **Concurrency**: simultaneous checkout of the last copy must be handled safely — no double-borrow
- **Consistency**: reservation queue notifications must serialize — two members must not both get the same copy
- **Auditability**: full immutable history of all borrow/return/fine events
- **Extensibility**: adding new book formats or fine policies must not require changes to core borrow/return flow
- **Availability**: catalog search should work read-only even if borrow service is down (eventual consistency acceptable for search)

---

## Constraints & Edge Cases

- A member cannot borrow the same title twice simultaneously
- Lost or damaged book: charged replacement cost; `BookItem` marked `LOST`/`DAMAGED`, removed from circulation
- Reservation queue must handle expiry and re-notification to next member automatically
- Fine payment is separate from the return flow — member can return the book but still owe a fine
- Multi-branch: each branch has its own inventory; inter-branch transfer is a separate workflow

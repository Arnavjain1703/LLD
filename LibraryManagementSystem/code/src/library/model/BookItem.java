package library.model;

import library.exception.BookNotAvailableException;
import library.exception.InvalidBookStateException;
import java.util.concurrent.atomic.AtomicInteger;

public class BookItem {

    // AtomicInteger — lock-free thread-safe counter
    private static final AtomicInteger counter = new AtomicInteger(1000);

    private final String barcode;
    private final Book book;       // back-reference to parent title
    private BookItemStatus status;

    public BookItem(Book book) {
        this.barcode = "BC" + counter.getAndIncrement();
        this.book    = book;
        this.status  = BookItemStatus.AVAILABLE;
    }

    // all state transitions synchronized — only one thread transitions at a time

    public synchronized void checkout() {
        if (status != BookItemStatus.AVAILABLE) {
            throw new BookNotAvailableException(
                "BookItem [" + barcode + "] cannot be checked out. Status: " + status
            );
        }
        this.status = BookItemStatus.BORROWED;
    }

    public synchronized void markReturned() {
        if (status != BookItemStatus.BORROWED) {
            throw new InvalidBookStateException(
                "BookItem [" + barcode + "] cannot be returned. Status: " + status
            );
        }
        this.status = BookItemStatus.AVAILABLE;
    }

    public synchronized void markReserved() {
        if (status != BookItemStatus.AVAILABLE) {
            throw new InvalidBookStateException(
                "BookItem [" + barcode + "] cannot be reserved. Status: " + status
            );
        }
        this.status = BookItemStatus.RESERVED;
    }

    public synchronized void markLost() {
        if (status == BookItemStatus.LOST) {
            throw new InvalidBookStateException(
                "BookItem [" + barcode + "] is already LOST."
            );
        }
        this.status = BookItemStatus.LOST;
    }

    public synchronized void markDamaged() {
        if (status == BookItemStatus.DAMAGED) {
            throw new InvalidBookStateException(
                "BookItem [" + barcode + "] is already DAMAGED."
            );
        }
        this.status = BookItemStatus.DAMAGED;
    }

    public synchronized boolean isAvailable() {
        return status == BookItemStatus.AVAILABLE;
    }

    public String getBarcode()                { return barcode; }
    public Book getBook()                     { return book; }
    public synchronized BookItemStatus getStatus() { return status; }

    // equality by barcode — barcode is the unique identity of a physical copy
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookItem)) return false;
        BookItem other = (BookItem) o;
        return this.barcode.equals(other.barcode);
    }

    @Override
    public int hashCode() {
        return barcode.hashCode();
    }

    @Override
    public String toString() {
        return "BookItem{barcode='" + barcode + "', title='" + book.getTitle() + "', status=" + status + "}";
    }
}

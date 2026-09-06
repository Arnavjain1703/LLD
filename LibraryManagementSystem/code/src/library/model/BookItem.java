package library.model;

import library.exception.BookNotAvailableException;
import library.exception.InvalidBookStateException;
import java.util.concurrent.atomic.AtomicInteger;

public class BookItem {

    // AtomicInteger — lock-free thread-safe counter
    // getAndIncrement() is a single atomic CPU instruction — no synchronized needed
    private static final AtomicInteger counter = new AtomicInteger(1000);

    private final String barcode;
    private final Book book;      // back-reference to parent title
    private BookItemStatus status;

    public BookItem(Book book) {
        this.barcode = "BC" + counter.getAndIncrement();
        this.book    = book;
        this.status  = BookItemStatus.AVAILABLE;
    }

    // all state transitions synchronized on `this`
    // only one thread can transition status at a time — prevents illegal state combinations

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

    public String getBarcode()               { return barcode; }
    public Book getBook()                    { return book; }
    public synchronized BookItemStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "BookItem{barcode='" + barcode + "', title='" + book.getTitle() + "', status=" + status + "}";
    }
}

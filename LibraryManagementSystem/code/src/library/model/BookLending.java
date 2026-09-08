package library.model;

import java.time.LocalDate;
import java.util.UUID;

public class BookLending {

    private static final int DEFAULT_BORROW_DAYS = 14;

    private final String    lendingId;
    private final BookItem  bookItem;
    private final Member    member;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private       LocalDate returnDate;  // null until returned

    public BookLending(BookItem bookItem, Member member) {
        this.lendingId  = UUID.randomUUID().toString();
        this.bookItem   = bookItem;
        this.member     = member;
        this.issueDate  = LocalDate.now();
        this.dueDate    = issueDate.plusDays(DEFAULT_BORROW_DAYS);
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isOverdue() {
        return !isReturned() && LocalDate.now().isAfter(dueDate);
    }

    /** Days overdue at the time of return (or today if not yet returned). */
    public long overdueDays() {
        LocalDate reference = isReturned() ? returnDate : LocalDate.now();
        return Math.max(0, reference.toEpochDay() - dueDate.toEpochDay());
    }

    public String    getLendingId()     { return lendingId; }
    public BookItem  getBookItem()      { return bookItem; }
    public Member    getMember()        { return member; }
    public LocalDate getIssueDate()     { return issueDate; }
    public LocalDate getDueDate()       { return dueDate; }
    public LocalDate getReturnDate()    { return returnDate; }

    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    @Override
    public String toString() {
        return "BookLending{id=" + lendingId
                + ", barcode=" + bookItem.getBarcode()
                + ", member=" + member.getEmail()
                + ", due=" + dueDate
                + ", returned=" + (isReturned() ? returnDate : "no") + "}";
    }
}

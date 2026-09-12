package library.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class BookReservation {

    private final String          reservationId;
    private final Book            book;
    private final Member          member;
    private final LocalDateTime   reservationDate;
    private       LocalDateTime   notifiedAt;
    private       ReservationStatus status;

    public BookReservation(Book book, Member member) {
        this.reservationId   = UUID.randomUUID().toString();
        this.book            = book;
        this.member          = member;
        this.reservationDate = LocalDateTime.now();
        this.status          = ReservationStatus.WAITING;
    }

    /** Called when a copy becomes available — transitions WAITING → NOTIFIED. */
    public void markNotified() {
        this.status     = ReservationStatus.NOTIFIED;
        this.notifiedAt = LocalDateTime.now();
    }

    /** Member collected the book — transitions NOTIFIED → COMPLETED. */
    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    /** Member cancels before collection. */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    /** Notification window expired without collection. */
    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public boolean isActive() {
        return status == ReservationStatus.WAITING
            || status == ReservationStatus.NOTIFIED;
    }

    public String            getReservationId()   { return reservationId; }
    public Book              getBook()            { return book; }
    public Member            getMember()          { return member; }
    public LocalDateTime     getReservationDate() { return reservationDate; }
    public LocalDateTime     getNotifiedAt()      { return notifiedAt; }
    public ReservationStatus getStatus()          { return status; }

    @Override
    public String toString() {
        return "BookReservation{id=" + reservationId
                + ", isbn=" + book.getIsbn()
                + ", member=" + member.getEmail()
                + ", status=" + status + "}";
    }
}

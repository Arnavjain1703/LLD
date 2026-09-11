package library.model;

import java.time.LocalDate;
import java.util.UUID;

public class Fine {

    private final String     fineId;
    private final BookLending lending;
    private final Member     member;
    private final double     amount;
    private       boolean    paid;
    private       boolean    waived;
    private final LocalDate  createdAt;

    public Fine(BookLending lending, Member member, double amount) {
        this.fineId    = UUID.randomUUID().toString();
        this.lending   = lending;
        this.member    = member;
        this.amount    = amount;
        this.paid      = false;
        this.waived    = false;
        this.createdAt = LocalDate.now();
    }

    public void pay() {
        if (isSettled()) throw new IllegalStateException("Fine already settled: " + fineId);
        this.paid = true;
    }

    public void waive() {
        if (isSettled()) throw new IllegalStateException("Fine already settled: " + fineId);
        this.waived = true;
    }

    public boolean isSettled() { return paid || waived; }

    public String      getFineId()    { return fineId; }
    public BookLending getLending()   { return lending; }
    public Member      getMember()    { return member; }
    public double      getAmount()    { return amount; }
    public boolean     isPaid()       { return paid; }
    public boolean     isWaived()     { return waived; }
    public LocalDate   getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Fine{id=" + fineId + ", member=" + member.getEmail()
                + ", amount=$" + String.format("%.2f", amount)
                + ", paid=" + paid + ", waived=" + waived + "}";
    }
}

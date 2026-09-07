package library.model;

/**
 * A library member. id == email (inherited from Person).
 * Tier LIBRARIAN grants elevated permissions (checked by FineService etc.).
 *
 * canBorrow() checks status only — whether the member has outstanding fines
 * is FineService's concern, checked separately in BorrowService.validateMember().
 */
public class Member extends Person {

    public static final int MAX_BORROW_LIMIT = 5;

    private MemberStatus status;
    private MemberTier   tier;

    public Member(String name, String email, String phone) {
        super(name, email, phone);
        this.status = MemberStatus.ACTIVE;
        this.tier   = MemberTier.REGULAR;
    }

    /** True when the member's account is ACTIVE (fine check is handled by FineService). */
    public boolean canBorrow() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isLibrarian() {
        return tier == MemberTier.LIBRARIAN;
    }

    public MemberStatus getStatus() { return status; }
    public MemberTier   getTier()   { return tier; }

    public void setStatus(MemberStatus status) { this.status = status; }
    public void setTier(MemberTier tier)       { this.tier   = tier; }

    @Override
    public String toString() {
        return "Member{email=" + email + ", name=" + name
                + ", status=" + status + ", tier=" + tier + "}";
    }
}

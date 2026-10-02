package model;

/**
 * Guest — a registered person who can make bookings.
 *
 * Immutable fields : guestId (system-assigned), email (login identity).
 * Mutable fields   : name, phone (legal name change / new number),
 *                    membershipTier (upgraded by loyalty engine).
 *
 * email is the natural key for deduplication — two accounts with the same
 * email are disallowed by GuestRepository.
 */
public class Guest {
    private final String guestId;          // system-assigned, immutable
    private final String email;            // login identity, immutable after registration
    private       String name;
    private       String phone;
    private       MembershipTier membershipTier;

    public Guest(String guestId, String name, String email, String phone) {
        this.guestId        = guestId;
        this.name           = name;
        this.email          = email;
        this.phone          = phone;
        this.membershipTier = MembershipTier.STANDARD;  // all guests start at STANDARD
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String         getGuestId()        { return guestId; }
    public String         getEmail()          { return email; }
    public String         getName()           { return name; }
    public String         getPhone()          { return phone; }
    public MembershipTier getMembershipTier() { return membershipTier; }

    // ── Setters (mutable fields only) ────────────────────────────────────────
    public void setName(String name)                       { this.name = name; }
    public void setPhone(String phone)                     { this.phone = phone; }
    public void setMembershipTier(MembershipTier tier)     { this.membershipTier = tier; }

    @Override
    public String toString() {
        return "Guest{id=" + guestId + ", name=" + name
             + ", email=" + email + ", tier=" + membershipTier + "}";
    }
}

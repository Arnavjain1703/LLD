package service;

import model.Guest;
import model.MembershipTier;
import repository.GuestRepository;

import java.util.List;

/**
 * GuestService -- guest lifecycle management.
 *
 * Responsibility: register, update profile, upgrade membership, deregister.
 * Actor: Guest (self-service) and Front Desk.
 *
 * Does NOT handle bookings -- that is BookingService.
 * Does NOT handle payments -- that is PaymentService.
 *
 * Invariants enforced:
 *   - Email is unique (dedup on register).
 *   - Membership can only move UP the tier ladder (no downgrade).
 *   - A guest with active bookings should not be deregistered -- caller
 *     (facade) is responsible for that guard; GuestService does not depend
 *     on BookingRepository to keep the dependency graph acyclic.
 */
public class GuestService {

    private final GuestRepository guestRepository;

    public GuestService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    /**
     * Register a new guest.
     * Guards: email must be unique across all guests.
     */
    public void registerGuest(Guest guest) {
        if (guestRepository.emailExists(guest.getEmail())) {
            throw new IllegalArgumentException(
                "A guest with email " + guest.getEmail() + " is already registered.");
        }
        guestRepository.save(guest);
        System.out.println("Registered: " + guest);
    }

    /**
     * Update mutable profile fields -- name and phone only.
     * Email is immutable post-registration (it is the identity key).
     */
    public void updateProfile(String guestId, String newName, String newPhone) {
        Guest guest = getGuest(guestId);
        if (newName != null && !newName.isBlank())   guest.setName(newName);
        if (newPhone != null && !newPhone.isBlank()) guest.setPhone(newPhone);
        System.out.println("Updated profile: " + guest);
    }

    /**
     * Upgrade membership tier.
     * Tiers can only increase (STANDARD -> SILVER -> GOLD -> PLATINUM).
     * Downgrade is a business policy violation -- throw to make it visible.
     */
    public void upgradeMembership(String guestId, MembershipTier newTier) {
        Guest guest = getGuest(guestId);
        if (newTier.ordinal() <= guest.getMembershipTier().ordinal()) {
            throw new IllegalArgumentException(
                "Cannot downgrade membership from " + guest.getMembershipTier()
                + " to " + newTier + " for guest: " + guestId);
        }
        guest.setMembershipTier(newTier);
        System.out.println("Membership upgraded: " + guest);
    }

    /** Lookup by ID -- used by BookingService and Front Desk. */
    public Guest getGuest(String guestId) {
        return guestRepository.findById(guestId)
            .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestId));
    }

    /** Lookup by email -- used on login / self-service flows. */
    public Guest getGuestByEmail(String email) {
        return guestRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No guest found with email: " + email));
    }

    /** All guests -- admin/reporting view. */
    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    /**
     * Deregister a guest.
     * Caller (facade) must ensure no active bookings exist before calling this.
     */
    public void deregisterGuest(String guestId) {
        getGuest(guestId); // guard: must exist
        guestRepository.delete(guestId);
        System.out.println("Deregistered guest: " + guestId);
    }
}

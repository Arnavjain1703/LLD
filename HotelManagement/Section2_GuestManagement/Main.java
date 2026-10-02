import model.*;
import repository.*;
import service.*;

public class Main {
    public static void main(String[] args) {

        // -- Repositories
        GuestRepository guestRepo = new GuestRepository();

        // -- Services
        GuestService guestService = new GuestService(guestRepo);

        // -- Register guests
        guestService.registerGuest(new Guest("G1", "Arjun Sharma", "arjun@email.com", "+91-9000000001"));
        guestService.registerGuest(new Guest("G2", "Priya Patel",  "priya@email.com",  "+91-9000000002"));
        guestService.registerGuest(new Guest("G3", "Wei Chen",     "wei@email.com",     "+1-415-0000003"));

        // -- Duplicate email guard
        System.out.println("\n=== Duplicate email test ===");
        try {
            guestService.registerGuest(new Guest("G99", "Fake Arjun", "arjun@email.com", "+00-000"));
        } catch (IllegalArgumentException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }

        // -- Update profile (name/phone only; email immutable)
        System.out.println("\n=== Update profile ===");
        guestService.updateProfile("G1", "Arjun K. Sharma", "+91-9111111111");

        // -- Membership upgrade
        System.out.println("\n=== Upgrade membership ===");
        guestService.upgradeMembership("G1", MembershipTier.SILVER);
        guestService.upgradeMembership("G1", MembershipTier.GOLD);

        // -- Downgrade guard
        System.out.println("\n=== Downgrade membership test ===");
        try {
            guestService.upgradeMembership("G1", MembershipTier.STANDARD);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }

        // -- Find by email
        System.out.println("\n=== Find by email ===");
        System.out.println(guestService.getGuestByEmail("priya@email.com"));

        // -- All guests
        System.out.println("\n=== All guests ===");
        guestService.getAllGuests().forEach(System.out::println);

        // -- Deregister
        System.out.println("\n=== Deregister G3 ===");
        guestService.deregisterGuest("G3");
        System.out.println("Total guests after deregister: " + guestService.getAllGuests().size()); // 2
    }
}

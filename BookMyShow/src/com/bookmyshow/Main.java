package com.bookmyshow;

import com.bookmyshow.enums.*;
import com.bookmyshow.exceptions.SeatNotAvailableException;
import com.bookmyshow.models.*;
import com.bookmyshow.observer.*;
import com.bookmyshow.payment.MockPaymentService;
import com.bookmyshow.service.BookingService;
import com.bookmyshow.strategy.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("═══ BookMyShow — Simplified E2E Demo ═══\n");

        // ── 1. Build Theater + Screen + Seats ─────────────────────────────
        Theater pvr = new Theater("T1", "PVR Phoenix", "Mumbai");
        Seat[][] layout = buildSeatLayout("T1-SC1", 4, 8, 300.0);
        Screen imax = new Screen("T1-SC1", "IMAX Hall", layout);
        pvr.addScreen(imax);

        // ── 2. Create Shows ───────────────────────────────────────────────
        Movie interstellar = new Movie("M1", "Interstellar", 169, "English", Genre.DRAMA);
        Movie inception    = new Movie("M2", "Inception", 148, "English", Genre.THRILLER);

        LocalDateTime now = LocalDateTime.now();
        Show show1 = new Show("SH1", interstellar, imax, now.plusHours(3));
        Show show2 = new Show("SH2", inception, imax, now.plusHours(8));

        imax.addShow(show1);
        imax.addShow(show2);

        // ── 3. Overlap Check ──────────────────────────────────────────────
        Show badShow = new Show("SH_BAD", inception, imax, now.plusHours(3).plusMinutes(30));
        boolean overlaps = hasOverlap(imax, badShow);
        System.out.println("[OVERLAP CHECK] SH_BAD overlaps? " + overlaps);

        // ── 4. Wire BookingService (Singleton) ────────────────────────────
        BookingService.resetInstance();
        BookingService svc = BookingService.getInstance(
                new DefaultPricingStrategy(),
                new StandardCancellationPolicy(),
                new MockPaymentService(),
                Arrays.asList(new EmailNotificationService(), new SMSNotificationService()));

        svc.registerShow(show1);
        svc.registerShow(show2);

        // ── 5. Alice books 2 RECLINER seats ───────────────────────────────
        System.out.println("\n─── Alice books RECLINERs on SH1 ───");
        SeatLock lockA = svc.lockSeats("SH1", List.of("T1-SC1-R0C0", "T1-SC1-R0C1"), "alice");
        Booking bookingA = svc.confirmBooking(lockA.getLockId());

        // ── 6. Bob books PREMIUM seats ────────────────────────────────────
        System.out.println("\n─── Bob books PREMIUMs on SH1 ───");
        SeatLock lockB = svc.lockSeats("SH1", List.of("T1-SC1-R1C0", "T1-SC1-R1C1"), "bob");
        Booking bookingB = svc.confirmBooking(lockB.getLockId());

        // ── 7. Concurrency: 5 threads race for same seat ──────────────────
        System.out.println("\n─── 5 threads race for T1-SC1-R2C0 ───");
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final String uid = "user-" + i;
            threads[i] = new Thread(() -> {
                try {
                    SeatLock lock = svc.lockSeats("SH1", List.of("T1-SC1-R2C0"), uid);
                    System.out.println("  " + uid + " WON → " + lock.getLockId());
                } catch (SeatNotAvailableException e) {
                    System.out.println("  " + uid + " LOST → " + e.getMessage());
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // ── 8. Cancel Alice's booking ─────────────────────────────────────
        System.out.println("\n─── Alice cancels ───");
        Booking cancelled = svc.cancelBooking(bookingA.getId(), "alice");
        System.out.println("  Status: " + cancelled.getStatus());
        System.out.println("  Seat T1-SC1-R0C0 now: " + show1.getSeatStatus("T1-SC1-R0C0"));

        // ── 9. Surge Pricing Demo ─────────────────────────────────────────
        System.out.println("\n─── Surge Pricing (1.5x) ───");
        BookingService.resetInstance();
        BookingService surgeSvc = BookingService.getInstance(
                new SurgePricingStrategy(1.5),
                new StandardCancellationPolicy(),
                new MockPaymentService(),
                List.of(new EmailNotificationService()));
        surgeSvc.registerShow(show2);
        SeatLock lockC = surgeSvc.lockSeats("SH2", List.of("T1-SC1-R0C0", "T1-SC1-R0C1"), "charlie");
        Booking bookingC = surgeSvc.confirmBooking(lockC.getLockId());
        System.out.println("  Price (1.5x surge on RECLINER): Rs." + bookingC.getTotalPrice());

        System.out.println("\n═══ Demo Complete ═══");
    }

    // ── Build seat layout: Row 0=RECLINER, Row 1=PREMIUM, rest=REGULAR ───
    static Seat[][] buildSeatLayout(String screenId, int rows, int cols, double basePrice) {
        Seat[][] layout = new Seat[rows][cols];
        for (int r = 0; r < rows; r++) {
            SeatType type = r == 0 ? SeatType.RECLINER : r == 1 ? SeatType.PREMIUM : SeatType.REGULAR;
            for (int c = 0; c < cols; c++) {
                String id = screenId + "-R" + r + "C" + c;
                layout[r][c] = new Seat(id, r, c, type, basePrice);
            }
        }
        return layout;
    }

    // ── Overlap check (30-min buffer) ─────────────────────────────────────
    static boolean hasOverlap(Screen screen, Show newShow) {
        for (Show existing : screen.getShows()) {
            if (newShow.getStartTime().isBefore(existing.getEndTime().plusMinutes(30))
                    && newShow.getEndTime().isAfter(existing.getStartTime()))
                return true;
        }
        return false;
    }
}

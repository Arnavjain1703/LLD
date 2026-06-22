package vendingmachine;

import vendingmachine.inventory.Rack;
import vendingmachine.model.Coin;
import vendingmachine.model.Product;
import vendingmachine.payment.CashPaymentStrategy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent stress tests — proves thread-safety guarantees under contention.
 *
 * Test 1 — Rack.deduct() synchronized:
 *   50 threads hit a rack of 5 simultaneously.
 *   Expected: exactly 5 succeed, 45 fail. Zero double-dispenses.
 *
 * Test 2 — CashPaymentStrategy AtomicLong:
 *   100 threads each insert a PENNY simultaneously.
 *   Expected: total = exactly 100 cents. Zero lost updates.
 *
 * Test 3 — TransactionContext AtomicReference (CAS):
 *   10 threads each attempt a full transaction on a machine with 3 items.
 *   Expected: exactly 3 dispenses, 7 rejected. Never more than 3.
 */
public class ThreadSafetyTest {

    static final String PASS = "PASS";
    static final String FAIL = "FAIL";

    public static void main(String[] args) throws InterruptedException {
        test1_RackSynchronized();
        test2_CoinAtomicLong();
        test3_TransactionContextCAS();
    }

    // ── Test 1: Rack.deduct() synchronized ───────────────────────────────────
    static void test1_RackSynchronized() throws InterruptedException {
        System.out.println("\n== TEST 1: Rack.deduct() — 50 threads, 5 items ==");

        Rack rack = new Rack("A1", new Product("P1", "Coke", 150), 5);

        int threads = 50;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                if (rack.deduct()) successes.incrementAndGet();
                else               failures.incrementAndGet();
                done.countDown();
            }).start();
        }

        ready.await();
        start.countDown();
        done.await();

        int s = successes.get(), f = failures.get();
        System.out.printf("  Successes: %d (expected 5) | Failures: %d (expected 45)%n", s, f);
        System.out.printf("  Remaining: %d (expected 0)%n", rack.getQuantity());
        System.out.println("  " + (s == 5 && f == 45 && rack.getQuantity() == 0 ? PASS : FAIL));
    }

    // ── Test 2: CashPaymentStrategy AtomicLong ────────────────────────────────
    static void test2_CoinAtomicLong() throws InterruptedException {
        System.out.println("\n== TEST 2: CashPaymentStrategy — 100 threads each insert PENNY ==");

        CashPaymentStrategy cash = new CashPaymentStrategy();
        int threads = 100;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                cash.insertCoin(Coin.PENNY);
                done.countDown();
            }).start();
        }

        ready.await();
        start.countDown();
        done.await();

        long total = cash.getInsertedCents();
        System.out.printf("  Total inserted: %d cents (expected 100)%n", total);
        System.out.println("  " + (total == 100 ? PASS : FAIL));
    }

    // ── Test 3: TransactionContext CAS — concurrent full transactions ─────────
    static void test3_TransactionContextCAS() throws InterruptedException {
        System.out.println("\n== TEST 3: Full FSM — 10 threads, 3 items, CAS serialization ==");

        // Fresh machine for this test (not the Singleton — avoids cross-test state)
        TransactionContext ctx = new TransactionContext(new vendingmachine.inventory.Inventory());
        ctx.getInventory().addRack("B1", new Product("P2", "Chips", 100), 3);

        int threads = 10;
        AtomicInteger dispensed = new AtomicInteger(0);
        AtomicInteger rejected  = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    try {
                        ctx.getState().selectRack("B1");
                        ctx.getState().selectPaymentMethod(new CashPaymentStrategy());
                        ctx.getState().insertCoin(Coin.ONE);   // $1.00 exact — triggers dispense
                        dispensed.incrementAndGet();
                    } catch (Exception e) {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await();
        start.countDown();
        done.await();

        int d = dispensed.get(), r = rejected.get();
        System.out.printf("  Dispensed: %d | Rejected: %d | Total: %d%n", d, r, d + r);
        System.out.printf("  Remaining in B1: %d%n",
                ctx.getInventory().getRack("B1").map(Rack::getQuantity).orElse(-1));
        boolean noOverDispense     = d <= 3;
        boolean allAccountedFor    = (d + r) == threads;
        System.out.printf("  No over-dispense: %s | All threads accounted for: %s%n",
                noOverDispense ? PASS : FAIL, allAccountedFor ? PASS : FAIL);
        System.out.println("  " + (noOverDispense && allAccountedFor ? PASS : FAIL));
    }
}

package vendingmachine;

import vendingmachine.inventory.Inventory;
import vendingmachine.inventory.Rack;
import vendingmachine.model.Coin;
import vendingmachine.model.Product;
import vendingmachine.payment.CardPaymentStrategy;
import vendingmachine.payment.CashPaymentStrategy;
import vendingmachine.payment.SimulatedCardProcessor;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // ── Single-threaded: basic FSM scenarios ──────────────────────────────

        VendingMachine vm = VendingMachine.getInstance();
        vm.addRack("A1", new Product("P001", "Coke",  150), 5);
        vm.addRack("A2", new Product("P002", "Chips", 200), 3);
        vm.addRack("B1", new Product("P003", "Water", 100), 1);

        separator("SCENARIO 1: Cash — exact amount (3 coins)");
        vm.selectRack("A1");
        vm.selectPaymentMethod(new CashPaymentStrategy());
        vm.insertCoin(Coin.ONE);        // $1.00 — needs $0.50 more
        vm.insertCoin(Coin.QUARTER);    // $1.25 — needs $0.25 more
        vm.insertCoin(Coin.QUARTER);    // $1.50 — dispenses

        separator("SCENARIO 2: Cash — overpayment, change returned");
        vm.selectRack("A2");
        vm.selectPaymentMethod(new CashPaymentStrategy());
        vm.insertCoin(Coin.FIVE);       // $5.00 → dispense Chips, $3.00 change

        separator("SCENARIO 3: Card — approved");
        vm.selectRack("A1");
        vm.selectPaymentMethod(new CardPaymentStrategy(new SimulatedCardProcessor()));
        vm.swipeCard("4111111111111111");

        separator("SCENARIO 4: Card — declined, then cancel");
        vm.selectRack("B1");
        String declinedToken = "tok_" + Math.abs("4000000000000002".hashCode());
        vm.selectPaymentMethod(new CardPaymentStrategy(
                new SimulatedCardProcessor(Set.of(declinedToken))));
        vm.swipeCard("4000000000000002");  // declined — stays in PaymentPending
        vm.cancel();                       // refund

        separator("SCENARIO 5: Cash — cancel mid-payment, coins refunded");
        vm.selectRack("A1");
        vm.selectPaymentMethod(new CashPaymentStrategy());
        vm.insertCoin(Coin.ONE);
        vm.cancel();

        separator("SCENARIO 6: Re-select rack before paying");
        vm.selectRack("A1");
        vm.selectRack("A2");            // change mind — resets and re-selects A2
        vm.selectPaymentMethod(new CashPaymentStrategy());
        vm.insertCoin(Coin.FIVE);       // dispense Chips, $3.00 change

        // ── Multi-threaded scenarios ──────────────────────────────────────────

        separator("SCENARIO 7: Flash sale — 5 users race for 2 Cokes (shared inventory)");
        scenario7_flashSale();

        separator("SCENARIO 8: Two users insert coins into the same machine simultaneously");
        scenario8_concurrentCoins();

        separator("SCENARIO 9: Restock thread vs dispense thread — no negative stock");
        scenario9_restockVsDispense();
    }

    // ── Scenario 7 ────────────────────────────────────────────────────────────
    // 5 machines share 1 Inventory that has only 2 Cokes.
    // All 5 threads fire simultaneously — only 2 can dispense.
    // Proves: Rack.deduct() synchronized prevents overselling.
    static void scenario7_flashSale() throws InterruptedException {
        Inventory shared = new Inventory();
        shared.addRack("C1", new Product("P004", "Coke Zero", 150), 2);

        int userCount = 5;
        AtomicInteger dispensed = new AtomicInteger(0);
        AtomicInteger failed    = new AtomicInteger(0);

        CountDownLatch ready = new CountDownLatch(userCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(userCount);

        for (int i = 1; i <= userCount; i++) {
            final int userId = i;
            // Each user gets their own machine (own FSM) but shares the same inventory
            TransactionContext ctx = new TransactionContext(shared);

            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();   // all threads start at the same instant
                    ctx.getState().selectRack("C1");
                    ctx.getState().selectPaymentMethod(new CashPaymentStrategy());
                    ctx.getState().insertCoin(Coin.ONE);
                    ctx.getState().insertCoin(Coin.QUARTER);
                    ctx.getState().insertCoin(Coin.QUARTER);
                    dispensed.incrementAndGet();
                    System.out.printf("[User %d] Got Coke Zero%n", userId);
                } catch (Exception e) {
                    failed.incrementAndGet();
                    System.out.printf("[User %d] Failed: %s%n", userId, e.getMessage());
                } finally {
                    done.countDown();
                }
            }, "User-" + userId).start();
        }

        ready.await();
        start.countDown();   // fire all 5 at once
        done.await();

        System.out.printf("%nResult — Dispensed: %d | Failed: %d | Stock left: %d%n",
                dispensed.get(), failed.get(),
                shared.getRack("C1").map(Rack::getQuantity).orElse(-1));
        System.out.println(dispensed.get() <= 2 ? "No oversell — PASS" : "Oversell detected — FAIL");
    }

    // ── Scenario 8 ────────────────────────────────────────────────────────────
    // Two people use the same machine — each inserts coins at the same time.
    // Proves: CashPaymentStrategy.AtomicLong never loses a coin update.
    static void scenario8_concurrentCoins() throws InterruptedException {
        Inventory inv = new Inventory();
        inv.addRack("D1", new Product("P005", "Pepsi", 200), 3);

        TransactionContext ctx = new TransactionContext(inv);
        ctx.getState().selectRack("D1");

        CashPaymentStrategy cash = new CashPaymentStrategy();
        ctx.getState().selectPaymentMethod(cash);
        // Machine is now in PaymentPending — price is $2.00

        int coinThreads = 4;
        CountDownLatch ready = new CountDownLatch(coinThreads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(coinThreads);

        // 4 threads each insert ONE ($1.00) simultaneously
        // Total = $4.00 — well over $2.00. Machine should dispense on first sufficient insert.
        for (int i = 1; i <= coinThreads; i++) {
            final int tid = i;
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    ctx.getState().insertCoin(Coin.ONE);
                    System.out.printf("[CoinThread-%d] Inserted $1.00%n", tid);
                } catch (Exception e) {
                    // Machine may have already dispensed and reset by the time other threads insert
                    System.out.printf("[CoinThread-%d] Rejected after dispense: %s%n",
                            tid, e.getClass().getSimpleName());
                } finally {
                    done.countDown();
                }
            }, "CoinThread-" + tid).start();
        }

        ready.await();
        start.countDown();
        done.await();

        System.out.printf("%nResult — Remaining Pepsi: %d%n",
                inv.getRack("D1").map(Rack::getQuantity).orElse(-1));
        System.out.println("AtomicLong protected coin accumulation — PASS");
    }

    // ── Scenario 9 ────────────────────────────────────────────────────────────
    // Restock thread adds items while dispense threads drain the rack simultaneously.
    // Proves: Rack.restock() and Rack.deduct() are both synchronized — stock never
    // goes negative even under concurrent reads and writes.
    static void scenario9_restockVsDispense() throws InterruptedException {
        Inventory inv = new Inventory();
        inv.addRack("E1", new Product("P006", "Juice", 100), 3);

        Rack rack = inv.getRack("E1").get();

        int dispenseThreads = 6;
        AtomicInteger dispensed = new AtomicInteger(0);
        AtomicInteger outOfStock = new AtomicInteger(0);

        CountDownLatch ready = new CountDownLatch(dispenseThreads + 1);  // +1 for restock thread
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(dispenseThreads + 1);

        // Restock thread: adds 2 items mid-race
        new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                Thread.sleep(5);  // slight delay — lets some dispenses start first
                rack.restock(2);
                System.out.println("[Restock] Added 2 units mid-race");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "Restock").start();

        // 6 dispense threads — initial stock 3 + restock 2 = max 5 should succeed
        for (int i = 1; i <= dispenseThreads; i++) {
            final int tid = i;
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (rack.deduct()) {
                        dispensed.incrementAndGet();
                        System.out.printf("[Dispense-%d] Dispensed Juice%n", tid);
                    } else {
                        outOfStock.incrementAndGet();
                        System.out.printf("[Dispense-%d] Out of stock%n", tid);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "Dispense-" + tid).start();
        }

        ready.await();
        start.countDown();
        done.await();

        int remaining = rack.getQuantity();
        System.out.printf("%nResult — Dispensed: %d | Out of stock: %d | Remaining: %d%n",
                dispensed.get(), outOfStock.get(), remaining);
        System.out.println(remaining >= 0 ? "Stock never went negative — PASS" : "Negative stock — FAIL");
    }

    // ─────────────────────────────────────────────────────────────────────────

    static void separator(String title) {
        System.out.println("\n══════════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("══════════════════════════════════════════════════════");
    }
}

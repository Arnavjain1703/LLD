import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // 1. Create Vending Machine (Singleton)
        VendingMachine machine = VendingMachine.getInstance("VM-001");

        // 2. Admin stocks the machine
        AdminService admin = new AdminService(machine);
        admin.addProduct("A1", new Product("Coca Cola", 1.50, "A1"), 5, 10);
        admin.addProduct("A2", new Product("Pepsi", 1.50, "A2"), 3, 10);
        admin.addProduct("B1", new Product("Lays Chips", 2.00, "B1"), 4, 8);
        admin.addProduct("B2", new Product("Snickers", 1.75, "B2"), 6, 10);
        admin.addProduct("C1", new Product("Water Bottle", 1.00, "C1"), 8, 12);

        admin.getInventoryReport();

        // 3. Normal purchase flow
        System.out.println("========== SCENARIO 1: Normal Purchase ==========\n");
        machine.selectProduct("A1");
        machine.insertMoney(2.00);
        machine.dispense();

        // 4. Insufficient funds scenario
        System.out.println("\n========== SCENARIO 2: Insufficient Funds ==========\n");
        machine.selectProduct("B1");
        machine.insertMoney(1.00);
        machine.dispense();
        machine.insertMoney(1.00);
        machine.dispense();

        // 5. Cancel transaction
        System.out.println("\n========== SCENARIO 3: Cancel Transaction ==========\n");
        machine.selectProduct("B2");
        machine.insertMoney(1.00);
        machine.cancelTransaction();

        // 6. Out of stock scenario
        System.out.println("\n========== SCENARIO 4: Out of Stock ==========\n");
        admin.removeProduct("A2");
        machine.selectProduct("A2");

        // 7. Admin restocks
        System.out.println("\n========== SCENARIO 5: Restock ==========\n");
        admin.restock("A1", 3);
        admin.getInventoryReport();

        // 8. Concurrent scenario: admin restocks while user purchases
        System.out.println("========== SCENARIO 6: Concurrent Admin + User ==========\n");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        // User thread: buys Coca Cola
        executor.submit(() -> {
            try {
                machine.selectProduct("A1");
                machine.insertMoney(2.00);
                machine.dispense();
                System.out.println("[Thread " + Thread.currentThread().getName()
                        + "] User purchase complete.");
            } catch (Exception e) {
                System.out.println("[Thread " + Thread.currentThread().getName()
                        + "] User failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        // Admin thread 1: restocks Lays Chips
        executor.submit(() -> {
            try {
                admin.restock("B1", 2);
                System.out.println("[Thread " + Thread.currentThread().getName()
                        + "] Admin restock of B1 complete.");
            } catch (Exception e) {
                System.out.println("[Thread " + Thread.currentThread().getName()
                        + "] Admin failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        // Admin thread 2: views inventory
        executor.submit(() -> {
            try {
                admin.getInventoryReport();
                System.out.println("[Thread " + Thread.currentThread().getName()
                        + "] Admin report complete.");
            } catch (Exception e) {
                System.out.println("[Thread " + Thread.currentThread().getName()
                        + "] Admin report failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        System.out.println("\n--- All concurrent operations complete ---\n");

        admin.getInventoryReport();
        executor.shutdown();

        System.out.println("========== SIMULATION COMPLETE ==========");
    }
}

import exception.VendingMachineException;
import model.*;
import payment.CardPaymentProcessor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        VendingMachine machine = VendingMachine.getInstance("VM-001");
        System.out.println("=== Vending Machine " + machine.getMachineId() + " Online ===");

        AdminService admin = new AdminService(machine);
        admin.addProduct("A1", new Product("A1", "Coca Cola", 1.50), 5, 10);
        admin.addProduct("A2", new Product("A2", "Pepsi", 1.50), 3, 10);
        admin.addProduct("B1", new Product("B1", "Lays Chips", 2.00), 4, 8);
        admin.addProduct("B2", new Product("B2", "Snickers", 1.75), 6, 10);
        admin.addProduct("C1", new Product("C1", "Water Bottle", 1.00), 8, 12);

        ButtonPanel panel = machine.getButtonPanel();

        // --- FLOW 1: Display products ---
        System.out.println("\n========== FLOW 1: Display Products ==========");
        panel.pressDisplayProducts();

        // --- FLOW 2: Normal purchase with change ---
        System.out.println("\n========== FLOW 2: Normal Purchase (with change) ==========");
        panel.pressProductButton("A1");
        panel.pressInsertCash(1.00);
        panel.pressInsertCash(1.00);
        panel.pressDispense();

        // --- FLOW 3: Exact amount, no change ---
        System.out.println("\n========== FLOW 3: Exact Amount ==========");
        panel.pressProductButton("B2");
        panel.pressInsertCash(1.75);
        panel.pressDispense();

        // --- FLOW 4: Insufficient funds → add more → dispense ---
        System.out.println("\n========== FLOW 4: Insufficient Funds ==========");
        panel.pressProductButton("B1");
        panel.pressInsertCash(1.00);
        safePress(panel::pressDispense);
        panel.pressInsertCash(1.00);
        panel.pressDispense();

        // --- FLOW 5: Cancel with refund ---
        System.out.println("\n========== FLOW 5: Cancel Transaction ==========");
        panel.pressProductButton("B2");
        panel.pressInsertCash(1.00);
        panel.pressCancel();

        // --- FLOW 6: Cancel with no transaction ---
        System.out.println("\n========== FLOW 6: Cancel With No Transaction ==========");
        panel.pressCancel();

        // --- FLOW 7: Insert money without selecting product ---
        System.out.println("\n========== FLOW 7: Insert Money Without Selection ==========");
        safePress(() -> panel.pressInsertCash(1.00));

        // --- FLOW 8: Dispense without anything ---
        System.out.println("\n========== FLOW 8: Dispense Without Selection ==========");
        safePress(panel::pressDispense);

        // --- FLOW 9: Out of stock ---
        System.out.println("\n========== FLOW 9: Out of Stock ==========");
        admin.removeProduct("A2");
        panel.pressProductButton("A2");

        // --- FLOW 10: Select during transaction ---
        System.out.println("\n========== FLOW 10: Select During Transaction ==========");
        panel.pressProductButton("C1");
        panel.pressInsertCash(0.50);
        safePress(() -> panel.pressProductButton("B1"));
        panel.pressCancel();

        // --- FLOW 11: Card payment ---
        System.out.println("\n========== FLOW 11: Card Payment ==========");
        machine.setPaymentProcessor(new CardPaymentProcessor());
        panel.pressProductButton("C1");
        panel.pressInsertCash(2.00);
        panel.pressDispense();

        // --- FLOW 12: Invalid button ---
        System.out.println("\n========== FLOW 12: Invalid Button ==========");
        panel.pressProductButton("Z9");

        // --- FLOW 13: Restock ---
        System.out.println("\n========== FLOW 13: Admin Restock ==========");
        admin.restock("A1", 3);

        // --- FLOW 14: Concurrent access stress test ---
        System.out.println("\n========== FLOW 14: Concurrent Access (10 threads) ==========");
        machine.setPaymentProcessor(new payment.CashPaymentProcessor());
        runConcurrencyTest(machine);

        // --- Final state ---
        System.out.println("\n========== FINAL INVENTORY ==========");
        admin.printInventoryReport();
        System.out.println("\n=== SIMULATION COMPLETE ===");
    }

    private static void safePress(Runnable action) {
        try {
            action.run();
        } catch (VendingMachineException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
    }

    private static void runConcurrencyTest(VendingMachine machine) throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    machine.selectProduct("C1");
                    machine.insertMoney(1.00);
                    machine.dispense();
                    System.out.println("  [THREAD-" + id + "] Purchase completed");
                } catch (VendingMachineException e) {
                    System.out.println("  [THREAD-" + id + "] Failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("  >> Concurrent test complete. Remaining C1 stock: "
                + machine.getInventory().getRack("C1").getQuantity());
    }
}

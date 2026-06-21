import models.*;
import models.PaymentProcessor.CardPaymentProcessor;

public class Main {
    public static void main(String[] args) {
        VendingMachine machine = VendingMachine.getInstance("VM-001");

        AdminService admin = new AdminService(machine);
        admin.addProduct("A1", new Product("Coca Cola", 1.50, "A1"), 5, 10);
        admin.addProduct("A2", new Product("Pepsi", 1.50, "A2"), 3, 10);
        admin.addProduct("B1", new Product("Lays Chips", 2.00, "B1"), 4, 8);
        admin.addProduct("B2", new Product("Snickers", 1.75, "B2"), 6, 10);
        admin.addProduct("C1", new Product("Water Bottle", 1.00, "C1"), 8, 12);

        ButtonPanel buttons = new ButtonPanel(machine);

        // ===== FLOW 1: Display products =====
        System.out.println("\n========== FLOW 1: Display Products ==========");
        buttons.pressDisplayProducts();

        // ===== FLOW 2: Normal purchase (user flow) =====
        System.out.println("\n========== FLOW 2: Normal Purchase ==========");
        buttons.pressProductButton("A1");
        buttons.pressInsertCash(1.00);
        buttons.pressInsertCash(1.00);
        buttons.pressDispense();

        // ===== FLOW 3: Insufficient funds then add more =====
        System.out.println("\n========== FLOW 3: Insufficient Funds ==========");
        buttons.pressProductButton("B1");
        buttons.pressInsertCash(1.00);
        buttons.pressDispense();
        buttons.pressInsertCash(1.00);
        buttons.pressDispense();

        // ===== FLOW 4: Cancel flow (refund) =====
        System.out.println("\n========== FLOW 4: Cancel Transaction ==========");
        buttons.pressProductButton("B2");
        buttons.pressInsertCash(1.00);
        buttons.pressCancel();

        // ===== FLOW 5: Cancel with no transaction =====
        System.out.println("\n========== FLOW 5: Cancel With No Transaction ==========");
        buttons.pressCancel();

        // ===== FLOW 6: Insert money without selecting product =====
        System.out.println("\n========== FLOW 6: Insert Money Without Selection ==========");
        buttons.pressInsertCash(1.00);

        // ===== FLOW 7: Dispense without anything =====
        System.out.println("\n========== FLOW 7: Dispense Without Selection ==========");
        buttons.pressDispense();

        // ===== FLOW 8: Out of stock =====
        System.out.println("\n========== FLOW 8: Out of Stock ==========");
        admin.removeProduct("A2");
        buttons.pressProductButton("A2");

        // ===== FLOW 9: Product selection while transaction in progress =====
        System.out.println("\n========== FLOW 9: Select During Transaction ==========");
        buttons.pressProductButton("C1");
        buttons.pressInsertCash(0.50);
        buttons.pressProductButton("B1");
        buttons.pressCancel();

        // ===== FLOW 10: Card payment =====
        System.out.println("\n========== FLOW 10: Card Payment ==========");
        machine.setPaymentProcessor(new CardPaymentProcessor());
        buttons.pressProductButton("C1");
        buttons.pressInsertCash(2.00);
        buttons.pressDispense();

        // ===== Display final inventory =====
        System.out.println("\n========== FINAL INVENTORY ==========");
        buttons.pressDisplayProducts();
        admin.getInventoryReport();

        System.out.println("========== SIMULATION COMPLETE ==========");
    }
}

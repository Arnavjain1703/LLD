package model;

import dispense.DispenseStrategy;
import dispense.StandardDispenseStrategy;
import payment.CashPaymentProcessor;
import payment.PaymentProcessor;
import state.IdleState;
import state.VendingMachineState;

/**
 * Thread-safety model:
 * - All user-facing operations (select, insert, dispense, cancel) are synchronized on `this`.
 *   This serializes the state machine transitions — no concurrent mutation of transaction state.
 * - Inventory uses ConcurrentHashMap for admin ops (add/remove rack) that don't participate in transactions.
 * - Rack uses AtomicInteger for quantity — safe for concurrent dispense across multiple machines sharing inventory.
 * - ButtonPanel uses ConcurrentHashMap for dynamic button registration by admin.
 * - PaymentProcessor and DispenseStrategy are set via synchronized setters (admin reconfiguration).
 */
public class VendingMachine {
    private static volatile VendingMachine instance;

    private final String machineId;
    private final Inventory inventory;
    private final Display display;
    private final ButtonPanel buttonPanel;

    private PaymentProcessor paymentProcessor;
    private DispenseStrategy dispenseStrategy;

    private VendingMachineState currentState;
    private double balance;
    private Product selectedProduct;

    private VendingMachine(String machineId) {
        this.machineId = machineId;
        this.inventory = new Inventory();
        this.display = new Display();
        this.buttonPanel = new ButtonPanel(this);
        this.paymentProcessor = new CashPaymentProcessor();
        this.dispenseStrategy = new StandardDispenseStrategy();
        this.currentState = new IdleState();
        this.balance = 0;
        this.selectedProduct = null;
    }

    public static VendingMachine getInstance(String machineId) {
        if (instance == null) {
            synchronized (VendingMachine.class) {
                if (instance == null) {
                    instance = new VendingMachine(machineId);
                }
            }
        }
        return instance;
    }

    // -- User-facing operations (synchronized for transaction atomicity) --

    public synchronized void selectProduct(String code) {
        currentState.selectProduct(this, code);
    }

    public synchronized void insertMoney(double amount) {
        if (amount <= 0) {
            display.showError("Amount must be positive.");
            return;
        }
        currentState.insertMoney(this, amount);
    }

    public synchronized void dispense() {
        currentState.dispense(this);
    }

    public synchronized void cancelTransaction() {
        currentState.cancel(this);
    }

    public synchronized void displayProducts() {
        display.showProducts(inventory);
    }

    // -- Transaction state mutators (called by states, already within synchronized block) --

    public void setState(VendingMachineState state) {
        this.currentState = state;
    }

    public void setSelectedProduct(Product product) {
        this.selectedProduct = product;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    public void resetTransaction() {
        this.balance = 0;
        this.selectedProduct = null;
        this.currentState = new IdleState();
        display.showWelcome();
    }

    // -- Accessors --

    public VendingMachineState getState() { return currentState; }
    public double getBalance() { return balance; }
    public Product getSelectedProduct() { return selectedProduct; }
    public String getMachineId() { return machineId; }
    public Inventory getInventory() { return inventory; }
    public Display getDisplay() { return display; }
    public ButtonPanel getButtonPanel() { return buttonPanel; }
    public PaymentProcessor getPaymentProcessor() { return paymentProcessor; }
    public DispenseStrategy getDispenseStrategy() { return dispenseStrategy; }

    public synchronized void setPaymentProcessor(PaymentProcessor processor) {
        this.paymentProcessor = processor;
    }

    public synchronized void setDispenseStrategy(DispenseStrategy strategy) {
        this.dispenseStrategy = strategy;
    }
}

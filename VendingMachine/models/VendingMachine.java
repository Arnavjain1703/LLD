package models;

import models.ItemDispenseStrategy.ItemDispenseStrategy;
import models.ItemDispenseStrategy.StandardDispenseStrategy;
import models.PaymentProcessor.CashPaymentProcessor;
import models.PaymentProcessor.PaymentProcessorInterface;
import models.State.IdleState;
import models.State.VendingMachineState;

public class VendingMachine {
    private static volatile VendingMachine instance;

    private String machineId;
    private VendingMachineState currentState;
    private Inventory inventory;
    private Display display;
    private double currentBalance;
    private Product selectedProduct;
    private PaymentProcessorInterface paymentProcessor;
    private ItemDispenseStrategy dispenseStrategy;

    private VendingMachine(String machineId) {
        this.machineId = machineId;
        this.inventory = new Inventory();
        this.display = new Display();
        this.currentState = new IdleState();
        this.currentBalance = 0;
        this.paymentProcessor = new CashPaymentProcessor();
        this.dispenseStrategy = new StandardDispenseStrategy();
        System.out.println("Vending Machine " + machineId + " initialized.");
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

    public synchronized void displayProducts() {
        display.showProducts(inventory);
    }

    public synchronized void selectProduct(String code) {
        currentState.selectProduct(this, code);
    }

    public synchronized void insertMoney(double amount) {
        currentState.insertMoney(this, amount);
    }

    public synchronized void dispense() {
        currentState.dispense(this);
    }

    public synchronized void cancelTransaction() {
        currentState.cancel(this);
    }

    public void setState(VendingMachineState state) {
        this.currentState = state;
    }

    public VendingMachineState getState() {
        return currentState;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Display getDisplay() {
        return display;
    }

    public double getBalance() {
        return currentBalance;
    }

    public void addBalance(double amount) {
        this.currentBalance += amount;
    }

    public void resetBalance() {
        this.currentBalance = 0;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product product) {
        this.selectedProduct = product;
    }

    public String getMachineId() {
        return machineId;
    }

    public PaymentProcessorInterface getPaymentProcessor() {
        return paymentProcessor;
    }

    public void setPaymentProcessor(PaymentProcessorInterface processor) {
        this.paymentProcessor = processor;
    }

    public ItemDispenseStrategy getDispenseStrategy() {
        return dispenseStrategy;
    }

    public void setDispenseStrategy(ItemDispenseStrategy strategy) {
        this.dispenseStrategy = strategy;
    }
}

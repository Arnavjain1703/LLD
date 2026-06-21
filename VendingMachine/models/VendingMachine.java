package models;

import models.ItemDispenseStrategy.ItemDispenseStrategy;
import models.ItemDispenseStrategy.StandardDispenseStrategy;
import models.PaymentProcessor.CashPaymentProcessor;
import models.PaymentProcessor.PaymentProcessorInterface;

public class VendingMachine {
    private static volatile VendingMachine instance;

    private String machineId;
    private Inventory inventory;
    private Display display;
    private PaymentProcessorInterface paymentProcessor;
    private ItemDispenseStrategy dispenseStrategy;
    private User currentUser;

    private VendingMachine(String machineId) {
        this.machineId = machineId;
        this.inventory = new Inventory();
        this.display = new Display();
        this.paymentProcessor = new CashPaymentProcessor();
        this.dispenseStrategy = new StandardDispenseStrategy();
        this.currentUser = new User();
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
        currentUser.getState().selectProduct(this, currentUser, code);
    }

    public synchronized void insertMoney(double amount) {
        currentUser.getState().insertMoney(this, currentUser, amount);
    }

    public synchronized void dispense() {
        currentUser.getState().dispense(this, currentUser);
    }

    public synchronized void cancelTransaction() {
        currentUser.getState().cancel(this, currentUser);
    }

    public String getMachineId() {
        return machineId;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Display getDisplay() {
        return display;
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

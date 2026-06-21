package models;

import models.State.IdleState;
import models.State.VendingMachineState;

public class VendingMachine {
    private static volatile VendingMachine instance;

    private String machineId;
    private VendingMachineState currentState;
    private Inventory inventory;
    private double currentBalance;
    private Product selectedProduct;

    private VendingMachine(String machineId) {
        this.machineId = machineId;
        this.inventory = new Inventory();
        this.currentState = new IdleState();
        this.currentBalance = 0;
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
}

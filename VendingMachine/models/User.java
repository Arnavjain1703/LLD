package models;

import models.State.IdleState;
import models.State.VendingMachineState;

public class User {
    private VendingMachineState currentState;
    private double currentBalance;
    private Product selectedProduct;

    public User() {
        this.currentState = new IdleState();
        this.currentBalance = 0;
        this.selectedProduct = null;
    }

    public VendingMachineState getState() {
        return currentState;
    }

    public void setState(VendingMachineState state) {
        this.currentState = state;
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

    public boolean isActive() {
        return selectedProduct != null;
    }
}

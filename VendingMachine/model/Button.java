package model;

public class Button {
    private final String label;
    private final Runnable action;

    public Button(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public void press() {
        System.out.println("\n[BUTTON] " + label);
        action.run();
    }

    public String getLabel() { return label; }
}

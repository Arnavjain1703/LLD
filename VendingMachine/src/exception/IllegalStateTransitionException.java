package exception;

public class IllegalStateTransitionException extends VendingMachineException {
    public IllegalStateTransitionException(String action, String state) {
        super("Cannot " + action + " in " + state + " state");
    }
}

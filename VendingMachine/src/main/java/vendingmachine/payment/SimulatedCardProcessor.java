package vendingmachine.payment;

/**
 * FOR TESTING / DEMO ONLY — not for production.
 *
 * In production, CardProcessor is implemented by the payment terminal SDK
 * (e.g. Stripe Terminal, Verifone SDK, Ingenico SDK) provided by the
 * hardware integrator. Our code never ships a real implementation — we only
 * define the interface.
 *
 * @deprecated Use src/test version. This file should not be in production sources.
 */
@Deprecated
public class SimulatedCardProcessor implements CardProcessor {

    private final java.util.Set<String> blockedTokens;

    public SimulatedCardProcessor(java.util.Set<String> blockedTokens) {
        this.blockedTokens = blockedTokens;
    }

    public SimulatedCardProcessor() { this(java.util.Set.of()); }

    @Override public String tokenize(String cardNumber) {
        return "tok_" + Math.abs(cardNumber.hashCode());
    }

    @Override public boolean charge(String cardToken, long amountInCents) {
        if (blockedTokens.contains(cardToken)) {
            System.out.printf("[SIM-CARD] DECLINED token %s%n", cardToken);
            return false;
        }
        System.out.printf("[SIM-CARD] APPROVED $%.2f on token %s%n", amountInCents / 100.0, cardToken);
        return true;
    }

    @Override public void refund(String cardToken, long amountInCents) {
        System.out.printf("[SIM-CARD] REFUNDED $%.2f to token %s%n", amountInCents / 100.0, cardToken);
    }
}
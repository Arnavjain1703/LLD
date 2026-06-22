package vendingmachine.payment;

import java.util.Set;

/**
 * FOR TESTING / DEMO ONLY — not for production.
 *
 * In production, CardProcessor is implemented by the payment terminal SDK
 * (e.g. Stripe Terminal, Verifone SDK, Ingenico SDK) provided by the
 * hardware integrator. Our code never ships a real implementation.
 */
public class SimulatedCardProcessor implements CardProcessor {

    private final Set<String> blockedTokens;

    public SimulatedCardProcessor(Set<String> blockedTokens) {
        this.blockedTokens = blockedTokens;
    }

    public SimulatedCardProcessor() {
        this(Set.of());
    }

    @Override
    public String tokenize(String cardNumber) {
        return "tok_" + Math.abs(cardNumber.hashCode());
    }

    @Override
    public boolean charge(String cardToken, long amountInCents) {
        if (blockedTokens.contains(cardToken)) {
            System.out.printf("[SIM-CARD] DECLINED token %s%n", cardToken);
            return false;
        }
        System.out.printf("[SIM-CARD] APPROVED $%.2f on token %s%n", amountInCents / 100.0, cardToken);
        return true;
    }

    @Override
    public void refund(String cardToken, long amountInCents) {
        System.out.printf("[SIM-CARD] REFUNDED $%.2f to token %s%n", amountInCents / 100.0, cardToken);
    }
}
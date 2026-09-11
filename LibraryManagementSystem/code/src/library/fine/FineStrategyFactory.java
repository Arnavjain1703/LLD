package library.fine;

import library.model.MemberTier;

public class FineStrategyFactory {

    public FineStrategy getStrategy(MemberTier tier) {
        return switch (tier) {
            case PREMIUM   -> new PremiumFineStrategy();
            case LIBRARIAN -> new WaivedFineStrategy();
            default        -> new RegularFineStrategy();
        };
    }
}

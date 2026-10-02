package model;

/**
 * Loyalty tier — drives discounts, priority booking, and amenity access.
 * Order matters: ordinal is used for "at least GOLD" comparisons.
 */
public enum MembershipTier {
    STANDARD,   // default on registration
    SILVER,
    GOLD,
    PLATINUM
}

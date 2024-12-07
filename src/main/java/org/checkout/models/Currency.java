package org.checkout.models;

public enum Currency {
    USD,
    GBP,
    EUR;

    public static boolean isValid(String code) {
        try {
            Currency.valueOf(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 
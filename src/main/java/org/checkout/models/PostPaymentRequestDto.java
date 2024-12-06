package org.checkout.models;

public record PostPaymentRequestDto(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv
) {}
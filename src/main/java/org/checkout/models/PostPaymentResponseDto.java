package org.checkout.models;

public record PostPaymentResponseDto(
    String id,
    String status,
    String lastFourCardDigits,
    int expiryMonth,
    int expiryYear,
    String currency,
    long amount
){}

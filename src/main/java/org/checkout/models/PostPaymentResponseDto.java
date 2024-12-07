package org.checkout.models;

public record PostPaymentResponseDto(
    String id,
    String status,
    String lastFourCardDigits,
    int expiryMonth,
    int expiryYear,
    String currency,
    long amount
){
    public static PostPaymentResponseDto fromPayment(Payment payment, String id, String status) {
        return new PostPaymentResponseDto(
                id,
                status,
                payment.getCardNumber().substring(payment.getCardNumber().length() - 4),
                payment.getExpiryMonth(),
                payment.getExpiryYear(),
                payment.getCurrency().toString(),
                payment.getAmountInMinorUnits()
        );
    }
}

package org.checkout.models;

import org.checkout.ValidationException;

import static org.checkout.PaymentValidator.validate;

public record Payment(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv
) {
    public static Payment fromPostPaymentRequestDto(PostPaymentRequestDto request) throws ValidationException {
        validate(request);
        return new Payment(
                request.cardNumber(),
                request.expiryMonth(),
                request.expiryYear(),
                request.currency(),
                request.amount(),
                request.cvv()
        );
    }
}
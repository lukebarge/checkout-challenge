package org.checkout.models;

public record PostPaymentRequestDto(
        String cardNumber,
        Integer expiryMonth,
        Integer expiryYear,
        String currency,
        Integer amount,
        String cvv
) {
    public String getMaskedCardNumber() {
        return cardNumber == null ? null : 
            "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getMaskedCvv() {
        return cvv == null ? null : "*".repeat(cvv.length());
    }

    @Override
    public String toString() {
        return "PostPaymentRequestDto{" +
            "cardNumber='" + getMaskedCardNumber() + '\'' +
            ", expiryMonth=" + expiryMonth +
            ", expiryYear=" + expiryYear +
            ", currency='" + currency + '\'' +
            ", amount=" + amount +
            ", cvv='" + getMaskedCvv() + '\'' +
            '}';
    }
}
package org.checkout.models;

import java.util.*;
import java.util.stream.Stream;

public final class Payment {

    // Card number is not serializable
    private final transient String cardNumber;

    private final Integer expiryMonth;
    private final Integer expiryYear;
    private final Money money;

    // CVV is not serializable
    private final transient String cvv;

    private Payment(String cardNumber, Integer expiryMonth, Integer expiryYear,
                    Money money, String cvv) {
        this.cardNumber = cardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.money = money;
        this.cvv = cvv;
    }

    public static ValidationResult<Payment> fromPostPaymentRequest(PostPaymentRequestDto request) {
        return Payment.create(request.cardNumber(), request.expiryMonth(), request.expiryYear(),
                request.currency(), request.amount(), request.cvv());
    }

    public static ValidationResult<Payment> create(String cardNumber, Integer expiryMonth,
                                                 Integer expiryYear, String currencyCode, 
                                                 Integer amountInMinorUnits, String cvv) {

        ValidationResult<String> cardNumberValidation = isValidCardNumber(cardNumber);
        ValidationResult<Integer> expiryMonthValidation = isValidExpiryMonth(expiryMonth);
        ValidationResult<Integer> expiryYearValidation = isValidExpiryYear(expiryYear);
        ValidationResult<?> expiryDateValidation = isValidExpiryDate(expiryMonth, expiryYear);
        ValidationResult<Money> moneyValidation = Money.of(amountInMinorUnits, currencyCode);
        ValidationResult<String> cvvValidation = isValidCvv(cvv);

        List<String> errors = Stream.of(
                cardNumberValidation,
                expiryMonthValidation,
                expiryYearValidation,
                expiryDateValidation,
                moneyValidation,
                cvvValidation
            )
            .filter(ValidationResult::isFailure)
            .flatMap(result -> result.errors().stream())
            .toList();

        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors);
        }

        return ValidationResult.success(new Payment(
            cardNumberValidation.getValue(),
            expiryMonthValidation.getValue(),
            expiryYearValidation.getValue(),
            moneyValidation.getValue(),
            cvvValidation.getValue()
        ));
    }

    private static ValidationResult<String> isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return ValidationResult.failure("Card number is required");
        }
        return cardNumber.matches("\\d{14,19}")
                ? ValidationResult.success(cardNumber)
                : ValidationResult.failure("Card number must be between 14-19 digits");
    }

    private static ValidationResult<Integer> isValidExpiryMonth(Integer month) {
        if (month == null) {
            return ValidationResult.failure("Expiry month is required");
        }
        return month >= 1 && month <= 12
                ? ValidationResult.success(month)
                : ValidationResult.failure("Expiry month must be between 1 and 12");
    }

    private static ValidationResult<Integer> isValidExpiryYear(Integer year) {
        if (year == null) {
            return ValidationResult.failure("Expiry year is required");
        }
        int currentYear = java.time.Year.now().getValue();
        return year >= currentYear
                ? ValidationResult.success(year)
                : ValidationResult.failure("Expiry year must be in the future");
    }

    private static ValidationResult<?> isValidExpiryDate(Integer month, Integer year) {
        try {
            java.time.YearMonth expiryDate = java.time.YearMonth.of(year, month);
            java.time.YearMonth now = java.time.YearMonth.now();
            
            return expiryDate.isAfter(now)
                    ? ValidationResult.success(expiryDate)
                    : ValidationResult.failure("Card expiry date must be in the future");
        } catch (java.time.DateTimeException e) {
            return ValidationResult.failure("Invalid expiry date");
        }
    }

    private static ValidationResult<String> isValidCvv(String cvv) {
        if (cvv == null) {
            return ValidationResult.failure("CVV is required");
        }
        return cvv.matches("\\d{3,4}")
                ? ValidationResult.success(cvv)
                : ValidationResult.failure("Invalid CVV");
    }

    // Getters
    public String getCardNumber() { 
        return cardNumber; 
    }
    
    public String getMaskedCardNumber() {
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
    public Integer getExpiryMonth() { return expiryMonth; }
    public Integer getExpiryYear() { return expiryYear; }
    public Integer getAmountInMinorUnits() { return money.getValueInMinorUnits(); }
    public Currency getCurrency() { return money.getCurrency(); }
    public String getCvv() { return cvv; }

    public String getMaskedCvv() {
        return "*".repeat(cvv.length());
    }

    @Override
    public String toString() {
        return "Payment{" +
            "cardNumber='" + getMaskedCardNumber() + '\'' +
            ", expiryMonth=" + expiryMonth +
            ", expiryYear=" + expiryYear +
            ", money=" + money.toDetailedString() +
            ", cvv='" + getMaskedCvv() + "'" +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Payment payment = (Payment) o;
        return expiryMonth == payment.expiryMonth &&
               expiryYear == payment.expiryYear &&
               Objects.equals(money, payment.money);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiryMonth, expiryYear, money);
    }

}


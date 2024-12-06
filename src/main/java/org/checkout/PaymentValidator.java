package org.checkout;

import org.checkout.models.PostPaymentRequestDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// PaymentValidator.java
public class PaymentValidator {
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");

    public static void validate(PostPaymentRequestDto request) throws ValidationException {
        List<ValidationError> errors = new ArrayList<>();

        validateCardNumber(request.cardNumber(), errors);
        validateExpiryDate(request.expiryMonth(), request.expiryYear(), errors);
        validateCurrency(request.currency(), errors);
        validateAmount(request.amount(), errors);
        validateCVV(request.cvv(), errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateCardNumber(String cardNumber, List<ValidationError> errors) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            errors.add(new ValidationError("cardNumber", "Card number is required"));
            return;
        }
        if (!cardNumber.matches("\\d{14,19}")) {
            errors.add(new ValidationError("cardNumber", "Card number must be 14-19 digits"));
        }
    }

    private static void validateExpiryDate(int month, int year, List<ValidationError> errors) {
        if (month < 1 || month > 12) {
            errors.add(new ValidationError("expiryMonth", "Expiry month must be between 1 and 12"));
        }

        LocalDate expiry = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        if (expiry.isBefore(LocalDate.now())) {
            errors.add(new ValidationError("expiryDate", "Card has expired"));
        }
    }

    private static void validateCurrency(String currency, List<ValidationError> errors) {
        if (currency == null || currency.length() != 3) {
            errors.add(new ValidationError("currency", "Currency must be 3 characters"));
            return;
        }
        if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            errors.add(new ValidationError("currency", "Unsupported currency. Must be one of: " +
                    String.join(", ", SUPPORTED_CURRENCIES)));
        }
    }

    private static void validateAmount(long amount, List<ValidationError> errors) {
        if (amount <= 0) {
            errors.add(new ValidationError("amount", "Amount must be greater than 0"));
        }
    }

    private static void validateCVV(String cvv, List<ValidationError> errors) {
        if (cvv == null || cvv.isEmpty()) {
            errors.add(new ValidationError("cvv", "CVV is required"));
            return;
        }
        if (!cvv.matches("\\d{3,4}")) {
            errors.add(new ValidationError("cvv", "CVV must be 3-4 digits"));
        }
    }
}

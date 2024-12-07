package org.checkout.models;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Money {
    private final Integer valueInMinorUnits;
    private final Currency currency;

    private Money(Integer valueInMinorUnits, Currency currency) {
        this.valueInMinorUnits = valueInMinorUnits;
        this.currency = currency;
    }

    public static ValidationResult<Money> of(Integer valueInMinorUnits, String currencyCode) {
        List<ValidationResult<?>> validations = new ArrayList<>();

        validations.add(isValidAmount(valueInMinorUnits));
        validations.add(isValidCurrency(currencyCode));

        List<String> errors = validations.stream()
                .filter(ValidationResult::isFailure)
                .flatMap(result -> result.errors().stream())
                .toList();

        return errors.isEmpty()
                ? ValidationResult.success(new Money(valueInMinorUnits, Currency.valueOf(currencyCode)))
                : ValidationResult.failure(errors);
    }

    private static ValidationResult<Integer> isValidAmount(Integer valueInMinorUnits) {
        if (valueInMinorUnits == null) {
            return ValidationResult.failure("Amount in minor units is required");
        }
        return valueInMinorUnits > 0
                ? ValidationResult.success(valueInMinorUnits)
                : ValidationResult.failure("Amount in minor units must be positive");
    }

    private static ValidationResult<Currency> isValidCurrency(String currencyCode) {
        if (currencyCode == null) {
            return ValidationResult.failure("Currency code is required");
        }

        if (!currencyCode.matches("[A-Z]{3}")) {
            return ValidationResult.failure("Currency code must be 3 uppercase letters");
        }

        try {
            return ValidationResult.success(Currency.valueOf(currencyCode));
        } catch (IllegalArgumentException e) {
            return ValidationResult.failure("Invalid currency code. Must be one of: " +
                    String.join(", ", Arrays.stream(Currency.values())
                            .map(Enum::name)
                            .toList()));
        }
    }

    public Integer getValueInMinorUnits() {
        return valueInMinorUnits;
    }

    public double getValueInMajorUnits() {
        return valueInMinorUnits / 100.0;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public String toString() {
        return getValueInMajorUnits() + " " + currency;
    }

    public String toDetailedString() {
        return valueInMinorUnits + " minor units (" +
                getValueInMajorUnits() + " " + currency + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(valueInMinorUnits, money.valueInMinorUnits) &&
                currency == money.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueInMinorUnits, currency);
    }
}
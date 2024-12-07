package org.checkout.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateValidMoney() {
        ValidationResult<Money> result = Money.of(1000, "USD");
        
        assertTrue(result.isSuccess());
        Money money = result.getValue();
        assertEquals(1000, money.getValueInMinorUnits());
        assertEquals(10.0, money.getValueInMajorUnits());
        assertEquals(Currency.USD, money.getCurrency());
    }

    @Test
    void shouldFailWithNullAmount() {
        ValidationResult<Money> result = Money.of(null, "USD");
        
        assertTrue(result.isFailure());
        assertTrue(result.errors().contains("Amount in minor units is required"));
    }

    @Test
    void shouldFailWithNegativeAmount() {
        ValidationResult<Money> result = Money.of(-100, "USD");
        
        assertTrue(result.isFailure());
        assertTrue(result.errors().contains("Amount in minor units must be positive"));
    }

    @Test
    void shouldFailWithInvalidCurrencyFormat() {
        ValidationResult<Money> result = Money.of(1000, "usd");
        
        assertTrue(result.isFailure());
        assertTrue(result.errors().contains("Currency code must be 3 uppercase letters"));
    }

    @Test
    void shouldFailWithUnsupportedCurrency() {
        ValidationResult<Money> result = Money.of(1000, "XYZ");
        
        assertTrue(result.isFailure());
        assertTrue(result.errors().getFirst().startsWith("Invalid currency code. Must be one of:"));
    }

    @Test
    void shouldCorrectlyFormatToString() {
        Money money = Money.of(1234, "USD").getValue();
        
        assertEquals("12.34 USD", money.toString());
        assertEquals("1234 minor units (12.34 USD)", money.toDetailedString());
    }

} 
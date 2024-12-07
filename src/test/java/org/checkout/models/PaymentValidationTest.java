package org.checkout.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class PaymentValidationTest {
    
    @Test
    @DisplayName("Should accept valid payment details")
    void shouldAcceptValidPayment() {
        ValidationResult<Payment> result = Payment.create(
            "1234567890123456",
            12,
            2025,
            "USD",
            1000,
            "123"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should reject invalid card number")
    void shouldRejectInvalidCardNumber() {
        ValidationResult<Payment> result = Payment.create(
            "123",  // invalid
            12,
            2025,
            "USD",
            1000,
            "123"
        );

        assertTrue(result.isFailure());
    }

    @Test
    @DisplayName("Should reject invalid expiry month")
    void shouldRejectInvalidExpiryMonth() {
        ValidationResult<Payment> result = Payment.create(
            "1234567890123456",
            13,                 // invalid
            2025,
            "USD",
            1000,
            "123"
        );

        assertTrue(result.isFailure());
        assertTrue(result.errors().contains("Expiry month must be between 1 and 12"));
    }

    @Test
    @DisplayName("Should reject invalid currency")
    void shouldRejectInvalidCurrency() {
        ValidationResult<Payment> result = Payment.create(
            "1234567890123456",
            12,
            2025,
            "US",               // invalid
            1000,
            "123"
        );

        assertTrue(result.isFailure());
    }

    @Test
    @DisplayName("Should reject non-positive amount")
    void shouldRejectInvalidAmount() {
        ValidationResult<Payment> result = Payment.create(
            "1234567890123456",
            12,
            2025,
            "USD",
            0,
            "123"
        );

        assertTrue(result.isFailure());
    }

    @Test
    @DisplayName("Should reject invalid CVV")
    void shouldRejectInvalidCvv() {
        ValidationResult<Payment> result = Payment.create(
            "1234567890123456",
            12,
            2025,
            "USD",
            1000,
            "12"
        );

        assertTrue(result.isFailure());
    }

    @Test
    @DisplayName("Should collect all validation errors")
    void shouldCollectMultipleErrors() {
        ValidationResult<Payment> result = Payment.create(
            "123",  // invalid
            13,   // invalid
            2025,
            "US",// invalid
            -1, // invalid
            "12" // invalid
        );

        assertTrue(result.isFailure());

        // This is 6 because the date has two sets of validation
        assertEquals(6, result.errors().size());
    }
}

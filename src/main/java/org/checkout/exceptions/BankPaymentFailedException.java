package org.checkout.exceptions;

public class BankPaymentFailedException extends RuntimeException {
    public BankPaymentFailedException(String message) {
        super(message);
    }
}
package org.checkout.exceptions;

public class IdempotencyKeyException extends RuntimeException {
    private final String idempotencyKey;

    public IdempotencyKeyException(String message, String idempotencyKey) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
package org.checkout;

import java.util.UUID;

public class PaymentIdGenerator implements IdGenerator {
    private static final String PAYMENT_PREFIX = "cko_";

    @Override
    public String generate() {
        return PAYMENT_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
} 
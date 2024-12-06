package org.checkout.repository;

import org.checkout.models.Payment;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;


public class PaymentsRepository {

    private final HashMap<String, Payment> payments = new HashMap<>();

    public void add(Payment payment) {
        payments.put(UUID.randomUUID().toString(), payment);
    }

    public Optional<Payment> get(String id) {
        return Optional.ofNullable(payments.get(id));
    }

}
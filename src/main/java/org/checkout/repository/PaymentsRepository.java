package org.checkout.repository;

import org.checkout.models.PostPaymentResponseDto;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;


public class PaymentsRepository implements IPaymentsRepository {

    private final HashMap<String, PostPaymentResponseDto> payments = new HashMap<>();

    public void add(PostPaymentResponseDto postPaymentResponseDto) {
        payments.put(postPaymentResponseDto.id(), postPaymentResponseDto);
    }

    public Optional<PostPaymentResponseDto> get(String id) {
        return Optional.ofNullable(payments.get(id));
    }

}
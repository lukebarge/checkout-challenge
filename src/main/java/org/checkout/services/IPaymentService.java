package org.checkout.services;

import org.checkout.models.Payment;
import org.checkout.models.PostPaymentResponseDto;

import java.util.Optional;

public interface IPaymentService {
    PostPaymentResponseDto processPayment(Payment payment, String idempotencyKey);
    Optional<PostPaymentResponseDto> getPaymentById(String id);
} 
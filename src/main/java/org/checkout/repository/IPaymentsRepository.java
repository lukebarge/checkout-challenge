package org.checkout.repository;

import org.checkout.models.PostPaymentResponseDto;
import java.util.Optional;

public interface IPaymentsRepository {
    void add(PostPaymentResponseDto postPaymentResponseDto);
    Optional<PostPaymentResponseDto> get(String id);
} 
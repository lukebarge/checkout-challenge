package org.checkout.services;

import org.checkout.IdGenerator;
import org.checkout.clients.BankClient;
import org.checkout.clients.BankSimulatorClient;
import org.checkout.exceptions.IdempotencyKeyException;
import org.checkout.models.Payment;
import org.checkout.models.PostPaymentResponseDto;
import org.checkout.repository.IPaymentsRepository;
import org.checkout.repository.IdempotentKeyStore;


import java.util.Optional;
import java.util.function.Supplier;

public class PaymentService implements IPaymentService {

    private final IdempotentKeyStore idempotencyKeys;
    private final IPaymentsRepository paymentsRepository;
    private final BankClient bankSimulatorClient;
    private final IdGenerator paymentIdGenerator;

    public PaymentService(IPaymentsRepository paymentsRepository, BankClient bankSimulatorClient, IdempotentKeyStore idempotencyKeys, IdGenerator paymentIdGenerator) {
        this.paymentsRepository = paymentsRepository;
        this.bankSimulatorClient = bankSimulatorClient;
        this.idempotencyKeys = idempotencyKeys;
        this.paymentIdGenerator = paymentIdGenerator;
    }

    public PostPaymentResponseDto processPayment(Payment payment, String idempotencyKey) {
        return processWithIdempotencyKey(idempotencyKey, () -> {
            String paymentId = paymentIdGenerator.generate();
            BankSimulatorClient.BankPaymentResponse bankPaymentResponse = bankSimulatorClient.makePayment(
                BankSimulatorClient.BankPaymentRequest.fromPayment(payment)
            );

            String status = bankPaymentResponse.authorized() ? "APPROVED" : "DECLINED";
            PostPaymentResponseDto response = PostPaymentResponseDto.fromPayment(payment, paymentId, status);

            // Only add the payment to the repository if the payment was successfully processed by the bank
            // otherwise the payment will not be stored
            paymentsRepository.add(response);
            return response;
        });
    }

    // Only add the idempotency key if the operation was successful
    private <T> T processWithIdempotencyKey(String idempotencyKey, Supplier<T> operation) {
        Optional.ofNullable(idempotencyKey).ifPresent(key -> {
            if (idempotencyKeys.contains(key)) {
                throw new IdempotencyKeyException("Idempotency key already is use", key);
            }
        });

        T result = operation.get();

        Optional.ofNullable(idempotencyKey).ifPresent(idempotencyKeys::add);
        
        return result;
    }

    public Optional<PostPaymentResponseDto>  getPaymentById(String id) {
        return paymentsRepository.get(id);
    }
}
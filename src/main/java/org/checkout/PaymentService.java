package org.checkout;

import org.checkout.clients.BankSimulatorClient;
import org.checkout.models.Payment;
import org.checkout.models.PostPaymentRequestDto;
import org.checkout.models.PostPaymentResponseDto;
import org.checkout.repository.PaymentsRepository;

import java.util.UUID;

public class PaymentService {

    private final PaymentsRepository paymentsRepository;
    private final BankSimulatorClient bankSimulatorClient;

    public PaymentService(PaymentsRepository paymentsRepository, BankSimulatorClient bankSimulatorClient) {
        this.paymentsRepository = paymentsRepository;
        this.bankSimulatorClient = bankSimulatorClient;
    }

    public PostPaymentResponseDto processPayment(Payment payment) throws Exception {
        BankSimulatorClient.PaymentResponse paymentResponse = bankSimulatorClient.makePayment(BankSimulatorClient.PaymentRequest.fromPayment(payment));


        return new PostPaymentResponseDto("arg1", "arg2", "arg3", 1, 2, "arg4", 123L); // Provide necessary arguments
    }

    public PostPaymentResponse getPaymentById(UUID id) {
        LOG.debug("Requesting access to to payment with ID {}", id);
        return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
    }

}
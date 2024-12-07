package org.checkout.clients;

import org.checkout.exceptions.BankPaymentFailedException;
import org.checkout.clients.BankSimulatorClient.BankPaymentRequest;
import org.checkout.clients.BankSimulatorClient.BankPaymentResponse;

public interface BankClient {
    /**
     * Makes a payment request to the bank.
     *
     * @param request The payment request details
     * @return The bank's response to the payment request
     * @throws BankPaymentFailedException if the payment fails or communication with bank fails
     */
    BankPaymentResponse makePayment(BankPaymentRequest request) throws BankPaymentFailedException;
} 
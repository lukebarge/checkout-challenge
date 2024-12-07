package org.checkout.clients;

import io.javalin.Javalin;
import org.checkout.clients.BankSimulatorClient.BankPaymentRequest;
import org.checkout.clients.BankSimulatorClient.BankPaymentResponse;
import org.checkout.exceptions.BankPaymentFailedException;
import org.checkout.models.Payment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BankSimulatorClientTest {
    
    private Javalin fakeBankApi;
    
    @AfterEach
    void tearDown() {
        if (fakeBankApi != null) {
            fakeBankApi.stop();
        }
    }

    private void testBankSimulator(TestFunction testBody) throws Exception {
        fakeBankApi = Javalin.create().start(0);
        String baseUrl = "http://localhost:" + fakeBankApi.port();
        BankSimulatorClient client = new BankSimulatorClient(baseUrl);
        testBody.apply(fakeBankApi, client);
    }

    @Test
    void successfulPayment() throws Exception {
        testBankSimulator((server, client) -> {
            server.post("/payments", ctx -> {
                ctx.contentType("application/json");
                ctx.result("""
                    {
                        "authorized": true,
                        "authorization_code": "AUTH123"
                    }
                """);
            });

            Payment payment =  Payment.create(
                "4242424242424242",
                12,
                2025,
                "USD",
                123,
                "123"
            ).getValue();

            BankPaymentResponse response = client.makePayment(
                BankPaymentRequest.fromPayment(payment)
            );

            assertTrue(response.authorized());
        });
    }

    @Test
    void failedPayment() throws Exception {
        testBankSimulator((server, client) -> {
            server.post("/payments", ctx -> {
                ctx.status(400);
                ctx.result("Payment failed");
            });

            Payment payment = Payment.create(
                    "4242424242424242",
                    12,
                    2025,
                    "USD",
                    123,
                    "123"
            ).getValue();

            assertThrows(BankPaymentFailedException.class, () -> {
                client.makePayment(BankPaymentRequest.fromPayment(payment));
            });
        });
    }

    @FunctionalInterface
    interface TestFunction {
        void apply(Javalin server, BankSimulatorClient client) throws Exception;
    }
}

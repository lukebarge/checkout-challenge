package org.checkout.functional;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.checkout.PaymentGatewayApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for the Payment Gateway application that verify the complete payment processing flow.
 * These tests use a fake bank API to simulate various scenarios and validate the application's behavior.
 */
public class PaymentGatewayApplicationTest {

    private PaymentGatewayApplication app;
    private Javalin fakeBankApi;

    private int getFutureYear() {
        return LocalDate.now().plusYears(2).getYear();
    }

    @BeforeEach
    void setUp() {
        fakeBankApi = Javalin.create().start(0);
        String baseUrl = "http://localhost:" + fakeBankApi.port();
        app = new PaymentGatewayApplication(baseUrl, () -> "cko_test123");
    }

    @AfterEach
    void tearDown() {
        if (fakeBankApi != null) {
            fakeBankApi.stop();
        }
    }

    @Test
    void shouldApprovePaymentWhenRequestIsValid() {
        fakeBankApi.post("/payments", ctx -> {
            ctx.contentType("application/json");
            ctx.result("""
                {
                    "authorized": true,
                    "authorization_code": "AUTH123"
                }
            """);
        });
        
        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String validPaymentJson = String.format("""
                {
                    "card_number": "4242424242424242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000,
                    "cvv": "123"
                }""", getFutureYear());
            
            // Create payment
            var createResponse = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "test-key-1");
            });
            assertThat(createResponse.code()).isEqualTo(200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode createJson = mapper.readTree(createResponse.body().string());
            
            // Retrieve payment
            var retrieveResponse = client.get("/api/payments/cko_test123");
            assertThat(retrieveResponse.code()).isEqualTo(200);
            
            JsonNode retrieveJson = mapper.readTree(retrieveResponse.body().string());
            
            // Verify both responses match expected format
            JsonNode expectedJson = mapper.readTree(String.format("""
                {
                    "id": "cko_test123",
                    "status": "APPROVED",
                    "last_four_card_digits": "4242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000
                }
                """, getFutureYear()));
            
            assertThat(createJson).isEqualTo(expectedJson);
            assertThat(retrieveJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void shouldRejectPaymentWhenRequestHasValidationErrors() {
        fakeBankApi.post("/payments", ctx -> {
            ctx.status(400);
            ctx.result("Payment failed");
        });

        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String invalidCardJson = """
                {
                    "card_number": "56",
                    "expiry_month": 12,
                    "expiry_year": 2020,
                    "currency": "JPY",
                    "amount": 10.0,
                    "cvv": "1234"
                }""";

            var response = client.post("/api/payments", invalidCardJson, requestBuilder -> {
                        requestBuilder.header("Content-Type", "application/json")
                                .header("Cko-Idempotency-Key", "test-key-2");
                    });

            assertThat(response.code()).isEqualTo(400);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualJson = mapper.readTree(response.body().string());
            JsonNode expectedJson = mapper.readTree("""
                {"errors":["Card number must be between 14-19 digits","Expiry year must be in the future","Card expiry date must be in the future","Invalid currency code. Must be one of: USD, GBP, EUR"],"message":"Payment rejected due to validation errors"}
                """);
            
            assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void shouldReturnDeclinedStatusWhenBankDeclines() {
        fakeBankApi.post("/payments", ctx -> {
            ctx.contentType("application/json");
            ctx.result("""
                {
                    "authorized": false,
                    "authorization_code": null
                }
            """);
        });

        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String validPaymentJson = String.format("""
                {
                    "card_number": "4242424242424242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000,
                    "cvv": "123"
                }""", getFutureYear());

            var response = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "test-key-3");
            });

            assertThat(response.code()).isEqualTo(200); // Payment Required

            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualJson = mapper.readTree(response.body().string());
            JsonNode expectedJson = mapper.readTree(String.format("""
                {
                    "id": "cko_test123",
                    "status": "DECLINED",
                    "last_four_card_digits": "4242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000
                }
                """, getFutureYear()));
            
            assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void shouldReturnErrorWhenBankResponseIsInvalid() {
        fakeBankApi.post("/payments", ctx -> {
            ctx.contentType("application/json");
            ctx.result("""
                {
                    "authorized": "not-a-boolean",
                    "invalid_field": "unexpected"
                }
            """);
        });

        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String validPaymentJson = String.format("""
                {
                    "card_number": "4242424242424242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000,
                    "cvv": "123"
                }""", getFutureYear());
            
            var response = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "test-key-4");
            });

            assertThat(response.code()).isEqualTo(500); // Bad Gateway
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualJson = mapper.readTree(response.body().string());
            JsonNode expectedJson = mapper.readTree("""
                {"error":"The bank returned an invalid response. The outcome of this payment is unknown"}
                """);
            
            assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void shouldReturnErrorWhenBankRejectsDueToInvalidDetails() {
        fakeBankApi.post("/payments", ctx -> {
            ctx.status(400);
            ctx.contentType("application/json");
            ctx.result("""
                {
                    "error": "Invalid payment details",
                    "code": "INVALID_REQUEST"
                }
                """);
        });

        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String validPaymentJson = String.format("""
                {
                    "card_number": "4242424242424242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000,
                    "cvv": "123"
                }""", getFutureYear());
            
            var response = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "test-key-5");
            });

            assertThat(response.code()).isEqualTo(500); // Bad Gateway
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualJson = mapper.readTree(response.body().string());
            JsonNode expectedJson = mapper.readTree("""
                {"error":"Bank rejected the payment request. This could be due to invalid payment details"}
                """);
            
            assertThat(actualJson).isEqualTo(expectedJson);
        });
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        fakeBankApi.post("/payments", ctx -> {
            ctx.contentType("application/json");
            ctx.result("""
                {
                    "authorized": true,
                    "authorization_code": "AUTH123"
                }
            """);
        });

        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String validPaymentJson = String.format("""
                {
                    "card_number": "4242424242424242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000,
                    "cvv": "123"
                }""", getFutureYear());
            
            // First request with idempotency key
            var firstResponse = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "duplicate-key");
            });
            assertThat(firstResponse.code()).isEqualTo(200);
            
            var secondResponse = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "duplicate-key");
            });

            assertThat(secondResponse.code()).isEqualTo(409);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualJson = mapper.readTree(secondResponse.body().string());
            JsonNode expectedJson = mapper.readTree("""
                {"idempotency_key":"duplicate-key","error":"Idempotency key already is use"}
                """);
            
            assertThat(actualJson).isEqualTo(expectedJson);

        });
    }

    @Test
    void shouldReturnErrorWhenBankIsUnavailable() {

        fakeBankApi.stop();

        JavalinTest.test(app.javalinApp(), (server, client) -> {
            String validPaymentJson = String.format("""
                {
                    "card_number": "4242424242424242",
                    "expiry_month": 12,
                    "expiry_year": %d,
                    "currency": "GBP",
                    "amount": 1000,
                    "cvv": "123"
                }""", getFutureYear());
            
            var response = client.post("/api/payments", validPaymentJson, requestBuilder -> {
                requestBuilder.header("Content-Type", "application/json")
                        .header("Cko-Idempotency-Key", "test-key-connection-error");
            });

            assertThat(response.code()).isEqualTo(500); // Bad Gateway
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualJson = mapper.readTree(response.body().string());
            JsonNode expectedJson = mapper.readTree("""
                {"error":"Unable to establish connection with bank. The payment was not processed"}
                """);
            
            assertThat(actualJson).isEqualTo(expectedJson);
        });
    }
}
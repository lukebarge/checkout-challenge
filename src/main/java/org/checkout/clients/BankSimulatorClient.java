package org.checkout.clients;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkout.models.Payment;

public class BankSimulatorClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public BankSimulatorClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public PaymentResponse makePayment(PaymentRequest request) throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/payments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("Payment failed with status code: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), PaymentResponse.class);
    }

    // Request POJO
   public record PaymentRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_date") String expiryDate,
    String currency,
    double amount,
    String cvv
) {
        public static PaymentRequest fromPayment(Payment payment) {
            return new PaymentRequest(
                    payment.cardNumber(),
                    payment.expiryMonth() + "/" + payment.expiryYear(),
                    payment.currency(),
                    payment.amount(),
                    payment.cvv()
            );
        }
    }

       // Response POJO
    public record PaymentResponse(
        boolean authorized,
        @JsonProperty("authorization_code") String authorizationCode
    ) {}
}

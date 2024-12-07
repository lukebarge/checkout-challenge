package org.checkout.clients;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.checkout.exceptions.BankPaymentFailedException;
import org.checkout.models.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import java.net.ConnectException;
import java.net.SocketException;

public class BankSimulatorClient implements BankClient {

    private static final Logger logger = LoggerFactory.getLogger(BankSimulatorClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public BankSimulatorClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public BankPaymentResponse makePayment(BankPaymentRequest request) throws BankPaymentFailedException {
        Request httpRequest = buildHttpRequest(request);
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Bank rejected the payment request {} with status code: {} and response {}", request, response.code(), response.body().string());
                throw new BankPaymentFailedException("Bank rejected the payment request. This could be due to invalid payment details");
            }
            
            return parseResponse(response);
        } catch (ConnectException e) {
            // Failed to establish initial connection
            logger.error("Unable to establish connection with bank", e);
            throw new BankPaymentFailedException("Unable to establish connection with bank. The payment was not processed");
        } catch (SocketException e) {
            // Connection was established but lost during transmission
            logger.error("Connection lost while communicating with bank", e);
            throw new BankPaymentFailedException("The outcome of this payment is unknown due to a communication error with the bank");
        } catch (IOException e) {
            // Other IO related errors
            logger.error("Bank communication error", e);
            throw new BankPaymentFailedException("The outcome of this payment is unknown due to a communication error with the bank");
        }
    }

    private Request buildHttpRequest(BankPaymentRequest request) throws BankPaymentFailedException {
        String jsonRequest;
        try {
            jsonRequest = objectMapper.writeValueAsString(request);
        } catch (IOException e) {
            logger.error("Failed to serialize bank payment request", e);
            throw new BankPaymentFailedException("Failed to prepare bank payment request. The payment was not processed");
        }

        try {
            return new Request.Builder()
                    .url(baseUrl + "/payments")
                    .post(RequestBody.create(jsonRequest, JSON))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.error("Failed to create bank HTTP request", e);
            throw new BankPaymentFailedException("Failed to prepare bank payment request. The payment was not processed");
        }
    }

    private BankPaymentResponse parseResponse(Response response) throws IOException, BankPaymentFailedException {
        if (response.body() == null) {
            logger.error("Bank returned empty response");
            throw new BankPaymentFailedException("The bank returned an empty response. The outcome of this payment is unknown");
        }

        String responseBody = response.body().string();
        try {
            return objectMapper.readValue(responseBody, BankPaymentResponse.class);
        } catch (IOException e) {
            logger.error("Failed to parse bank response: {}", responseBody, e);
            throw new BankPaymentFailedException("The bank returned an invalid response. The outcome of this payment is unknown");
        }
    }

   public record BankPaymentRequest(
    String cardNumber,
    String expiryDate,
    String currency,
    Integer amount,
    String cvv
) {
    @Override
    public String toString() {
        return "BankPaymentRequest[" +
                "cardNumber=" + maskCardNumber(cardNumber) +
                ", expiryDate=" + expiryDate +
                ", currency=" + currency +
                ", amount=" + amount +
                ", cvv=***]";
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }

    public static BankPaymentRequest fromPayment(Payment payment) {
        return new BankPaymentRequest(
                payment.getCardNumber(),
                String.format("%02d/%s", payment.getExpiryMonth(), payment.getExpiryYear()),
                payment.getCurrency().toString(),
                payment.getAmountInMinorUnits(),
                payment.getCvv()
        );
    }
}

    public record BankPaymentResponse(
        boolean authorized,
        String authorizationCode
    ) {}
}

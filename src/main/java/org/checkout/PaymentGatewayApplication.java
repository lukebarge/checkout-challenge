package org.checkout;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.ValidationException;
import org.checkout.clients.BankClient;
import org.checkout.clients.BankSimulatorClient;
import org.checkout.controllers.PaymentGatewayController;
import org.checkout.exceptions.BankPaymentFailedException;
import org.checkout.exceptions.IdempotencyKeyException;
import org.checkout.models.ValidationResult;
import org.checkout.repository.IPaymentsRepository;
import org.checkout.repository.IdempotentKeyStore;
import org.checkout.repository.IdempotentKeyStoreRepository;
import org.checkout.repository.PaymentsRepository;
import org.checkout.services.IPaymentService;
import org.checkout.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class PaymentGatewayApplication {
    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayApplication.class);
    private static final Properties properties = loadProperties();

    private final PaymentGatewayController paymentGatewayController;
    private final Javalin app;

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = PaymentGatewayApplication.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            logger.warn("Could not load application.properties, using defaults", e);
        }
        return props;
    }

    public PaymentGatewayApplication() {
        this(properties.getProperty("bank.simulator.url", "http://localhost:8080"));
    }

    public PaymentGatewayApplication(String bankSimulatorBaseUrl) {
        this(bankSimulatorBaseUrl, new PaymentIdGenerator());
    }

    public PaymentGatewayApplication(String bankSimulatorBaseUrl, IdGenerator idGenerator) {
        IdempotentKeyStore idempotencyKeys = new IdempotentKeyStoreRepository();
        IPaymentsRepository paymentsRepository = new PaymentsRepository();
        BankClient bankSimulatorClient = new BankSimulatorClient(bankSimulatorBaseUrl);
        IPaymentService paymentService = new PaymentService(paymentsRepository, bankSimulatorClient, idempotencyKeys, idGenerator);
        this.paymentGatewayController = new PaymentGatewayController(paymentService);

        this.app = configureJavalin();
    }

    public Javalin javalinApp() {
        return app;
    }

    private Javalin configureJavalin() {
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            }));
        });

        configureRoutes(app);
        configureExceptionHandling(app);

        return app;
    }

    private void configureRoutes(Javalin app) {
        app.post("/api/payments", paymentGatewayController::postPayment);
        app.get("/api/payments/{id}", paymentGatewayController::getPaymentById);
    }

    private void configureExceptionHandling(Javalin app) {
        app.exception(ValidationResult.ValidationException.class, (e, ctx) -> {
            logger.error("Validation error: {}", e.getErrors());
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of(
                    "message", "Payment rejected due to validation errors",
                    "errors", e.getErrors()
            ));
        });

        app.exception(ValidationException.class, (e, ctx) -> {
            logger.error("Validation error: {}", e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST);
        });

        app.exception(BankPaymentFailedException.class, (e, ctx) -> {
            logger.error("Unexpected error occurred", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(Map.of("error", e.getMessage()));
        });

        app.exception(IdempotencyKeyException.class, (e, ctx) -> {
            logger.error("Unexpected error occurred", e);
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", e.getMessage(), "idempotency_key", e.getIdempotencyKey()));
        });
    }

    public static void main(String[] args) {
        PaymentGatewayApplication application = new PaymentGatewayApplication();
        int port = Integer.parseInt(properties.getProperty("server.port", "7070"));
        application.javalinApp().start(port);
    }
}


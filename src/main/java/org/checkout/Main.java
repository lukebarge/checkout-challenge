package org.checkout;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;

import org.checkout.models.Payment;
import org.checkout.models.PostPaymentRequestDto;
import org.checkout.models.PostPaymentResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        PaymentService paymentService = new PaymentService();

        Javalin app = Javalin.create().start(7070);

        app.post("/api/payments", ctx -> {
            PostPaymentRequestDto request = ctx.bodyAsClass(PostPaymentRequestDto.class);

            Payment payment = Payment.fromPostPaymentRequestDto(request);

            PostPaymentResponseDto response = paymentService.processPayment(payment);
            ctx.json(response);

        });

        app.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of(
                    "message", "Validation failed",
                    "errors", e.getErrors()
            ));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(Map.of("error", "An unexpected error occurred"));
        });

        app.error(404, ctx -> {
            ctx.result("Generic 404 message");
        });

        app.get("/api/payments/{id}", ctx -> {
            String id = ctx.pathParam("id");
            ctx.json(Map.of("message", "Payment details retrieval not implemented"));
        });

        app.get("/", ctx -> {
            ctx.result("Hello World");
        });
    }
}


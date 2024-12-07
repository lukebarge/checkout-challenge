package org.checkout.controllers;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.checkout.services.IPaymentService;
import org.checkout.exceptions.BankPaymentFailedException;
import org.checkout.models.Payment;
import org.checkout.models.PostPaymentRequestDto;
import org.checkout.models.PostPaymentResponseDto;
import org.checkout.models.ValidationResult.ValidationException;

public class PaymentGatewayController implements IPaymentGatewayController {

    private final IPaymentService paymentService;

    public PaymentGatewayController(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void postPayment(Context ctx) throws BankPaymentFailedException, ValidationException {
        PostPaymentRequestDto postPaymentRequestDto = ctx.bodyValidator(PostPaymentRequestDto.class)
            .check(dto -> dto.cardNumber() != null, "Card number is required")
            .check(dto -> dto.expiryMonth() != null, "Expiry month is required")
            .check(dto -> dto.expiryYear() != null, "Expiry year is required")
            .check(dto -> dto.currency() != null, "Currency is required")
            .check(dto -> dto.amount() != null, "Amount is required")
            .check(dto -> dto.cvv() != null, "CVV is required")
            .get();
        
        Payment payment = Payment.fromPostPaymentRequest(postPaymentRequestDto).getValue();
        String idempotentKey = ctx.header("Cko-Idempotency-Key");
        PostPaymentResponseDto response = paymentService.processPayment(payment, idempotentKey);
        ctx.json(response);
    }

    public void getPaymentById(Context ctx) {
        String id = ctx.pathParam("id");
        paymentService.getPaymentById(id).ifPresentOrElse(
            ctx::json,
            () -> ctx.status(HttpStatus.NOT_FOUND)
        );
    }
}
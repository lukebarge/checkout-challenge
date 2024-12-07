package org.checkout.controllers;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.BodyValidator;
import org.checkout.services.IPaymentService;
import org.checkout.models.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayControllerTest {

    @Mock
    private IPaymentService paymentService;
    
    @Mock
    private Context ctx;

    private PaymentGatewayController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentGatewayController(paymentService);
    }

    @Test
    void shouldProcessPaymentSuccessfullyWhenRequestIsValid() {
        PostPaymentRequestDto requestDto = new PostPaymentRequestDto(
            "4242424242424242",
            12,
            2025,
            "USD",
            100,
            "123"
        );

        PostPaymentResponseDto expectedResponse = new PostPaymentResponseDto(
            "cko_123",
            "approved",
            "4242",
            12,
            2025,
            "USD",
            100
        );

        when(ctx.bodyValidator(PostPaymentRequestDto.class)).thenReturn(new TestBodyValidator(requestDto));
        when(ctx.header("Cko-Idempotency-Key")).thenReturn("idem_key_123");
        when(paymentService.processPayment(any(Payment.class), eq("idem_key_123"))).thenReturn(expectedResponse);

        controller.postPayment(ctx);

        verify(paymentService).processPayment(any(Payment.class), eq("idem_key_123"));
        verify(ctx).json(expectedResponse);
    }

    @Test
    void shouldReturnPaymentWhenPaymentExists() {

        String paymentId = "cko_123";
        PostPaymentResponseDto payment = new PostPaymentResponseDto(
            paymentId,
            "approved",
            "4242",
            12,
            2025,
            "USD",
            100
        );
        
        when(ctx.pathParam("id")).thenReturn(paymentId);
        when(paymentService.getPaymentById(paymentId)).thenReturn(Optional.of(payment));

        controller.getPaymentById(ctx);

        verify(ctx).json(payment);
        verify(ctx, never()).status(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnNotFoundWhenPaymentDoesNotExist() {
        String paymentId = "non_existing_payment";
        
        when(ctx.pathParam("id")).thenReturn(paymentId);
        when(paymentService.getPaymentById(paymentId)).thenReturn(Optional.empty());

        controller.getPaymentById(ctx);

        verify(ctx).status(HttpStatus.NOT_FOUND);
        verify(ctx, never()).json(any());
    }

    private static class TestBodyValidator extends BodyValidator<PostPaymentRequestDto> {
        private final PostPaymentRequestDto dto;

        TestBodyValidator(PostPaymentRequestDto dto) {
            super("", PostPaymentRequestDto.class, () -> null);
            this.dto = dto;
        }

        @NotNull
        @Override
        public PostPaymentRequestDto get() {
            return dto;
        }
    }
} 
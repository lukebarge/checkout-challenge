package org.checkout.controllers;

import io.javalin.http.Context;
import org.checkout.exceptions.BankPaymentFailedException;
import org.checkout.models.ValidationResult.ValidationException;

public interface IPaymentGatewayController {
    void postPayment(Context ctx) throws BankPaymentFailedException, ValidationException;
    void getPaymentById(Context ctx);
} 
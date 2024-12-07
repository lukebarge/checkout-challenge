package org.checkout.services;

import org.checkout.IdGenerator;
import org.checkout.clients.BankClient;
import org.checkout.clients.BankSimulatorClient;
import org.checkout.exceptions.IdempotencyKeyException;
import org.checkout.models.Payment;
import org.checkout.models.PostPaymentResponseDto;
import org.checkout.repository.IPaymentsRepository;
import org.checkout.repository.IdempotentKeyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private IPaymentsRepository paymentsRepository;
    @Mock
    private BankClient bankClient;
    @Mock
    private IdempotentKeyStore idempotencyKeys;
    @Mock
    private IdGenerator paymentIdGenerator;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentsRepository, bankClient, idempotencyKeys, paymentIdGenerator);
    }

    @Test
    void processPaymentWithValidPaymentAndIdempotencyKeyShouldApproveAndSavePayment() {
        // Arrange
        Payment payment = Payment.create(
            "4242424242424242",    
            12,                   
            2025,                  
            "USD",                 
            10000,                 
            "123"                 
        ).getValue();
        String idempotencyKey = "test-key-1";
        String generatedPaymentId = "payment-123";
        
        when(paymentIdGenerator.generate()).thenReturn(generatedPaymentId);
        when(bankClient.makePayment(any())).thenReturn(new BankSimulatorClient.BankPaymentResponse(true, "AUTH123"));
        when(idempotencyKeys.contains(idempotencyKey)).thenReturn(false);

        PostPaymentResponseDto response = paymentService.processPayment(payment, idempotencyKey);

        assertEquals("APPROVED", response.status());
        assertEquals(generatedPaymentId, response.id());
        verify(paymentsRepository).add(any(PostPaymentResponseDto.class));
        verify(idempotencyKeys).add(idempotencyKey);
    }

    @Test
    void processPaymentWithDeclinedBankResponseShouldDeclineAndSavePayment() {
        Payment payment = Payment.create(
            "4242424242424242",
            12,
            2025,
            "USD",
            10000,
            "123"
        ).getValue();
        String idempotencyKey = "test-key-2";
        String generatedPaymentId = "payment-456";
        
        when(paymentIdGenerator.generate()).thenReturn(generatedPaymentId);
        when(bankClient.makePayment(any())).thenReturn(new BankSimulatorClient.BankPaymentResponse(false, null));
        when(idempotencyKeys.contains(idempotencyKey)).thenReturn(false);

        PostPaymentResponseDto response = paymentService.processPayment(payment, idempotencyKey);

        // Assert
        assertEquals("DECLINED", response.status());
        assertEquals(generatedPaymentId, response.id());
        verify(paymentsRepository).add(any(PostPaymentResponseDto.class));
        verify(idempotencyKeys).add(idempotencyKey);
    }

    @Test
    void processPaymentWithExistingIdempotencyKeyShouldThrowException() {
        Payment payment = Payment.create(
            "4242424242424242",
            12,
            2025,
            "USD",
            10000,
            "123"
        ).getValue();
        String idempotencyKey = "test-key-3";
        
        when(idempotencyKeys.contains(idempotencyKey)).thenReturn(true);

        assertThrows(IdempotencyKeyException.class,
            () -> paymentService.processPayment(payment, idempotencyKey));
        
        verify(bankClient, never()).makePayment(any());
        verify(paymentsRepository, never()).add(any());
    }

    @Test
    void processPaymentWithNullIdempotencyKeyShouldProcessWithoutIdempotencyCheck() {
        Payment payment = Payment.create(
            "4242424242424242",
            12,
            2025,
            "USD",
            10000,
            "123"
        ).getValue();
        String generatedPaymentId = "payment-789";
        
        when(paymentIdGenerator.generate()).thenReturn(generatedPaymentId);
        when(bankClient.makePayment(any())).thenReturn(new BankSimulatorClient.BankPaymentResponse(true, "AUTH123"));

        PostPaymentResponseDto response = paymentService.processPayment(payment, null);

        assertEquals("APPROVED", response.status());
        assertEquals(generatedPaymentId, response.id());
        verify(paymentsRepository).add(any(PostPaymentResponseDto.class));
        verify(idempotencyKeys, never()).add(any());
    }

    @Test
    void getPaymentByIdWithExistingPaymentShouldReturnPayment() {
       String paymentId = "payment-123";
        PostPaymentResponseDto expectedPayment = new PostPaymentResponseDto(
            paymentId,                
            "APPROVED",              
            "4242",                  
            12,                      
            2025,                   
            "USD",                  
            10000                   
        );
        when(paymentsRepository.get(paymentId)).thenReturn(Optional.of(expectedPayment));

        Optional<PostPaymentResponseDto> result = paymentService.getPaymentById(paymentId);

        assertTrue(result.isPresent());
        assertEquals(expectedPayment, result.get());
    }

    @Test
    void getPaymentByIdWithNonExistentPaymentShouldReturnEmpty() {
        String paymentId = "non-existent";
        when(paymentsRepository.get(paymentId)).thenReturn(Optional.empty());

        Optional<PostPaymentResponseDto> result = paymentService.getPaymentById(paymentId);

        assertTrue(result.isEmpty());
    }
} 
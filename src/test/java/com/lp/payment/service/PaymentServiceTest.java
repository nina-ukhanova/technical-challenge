package com.lp.payment.service;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.external.ExternalSystemMock;
import com.lp.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ExternalSystemMock externalSystemMock;

    private PaymentService paymentService;

    @BeforeEach
    void init() {
        paymentService = new PaymentService(paymentRepository, externalSystemMock);
    }

    @Test
    void testProcessPayment_AmountInvalid() {

        // Case 1. - negative amount
        final PaymentRequest requestInvalidAmount = new PaymentRequest();
        requestInvalidAmount.setCardHolder("Test User");
        requestInvalidAmount.setAmount(-500); // TODO to make a constant
        requestInvalidAmount.setCurrency("EUR"); // TODO to use enum
        requestInvalidAmount.setCardNumber("1234567890123456");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> paymentService.processPayment(requestInvalidAmount));

        verify(paymentRepository, never()).findByIdempotencyKey(anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(externalSystemMock, never()).sendPayment(any(Payment.class));

        // Case 2. - amount more then 199999999
        requestInvalidAmount.setAmount(200000000); // TODO to make a constant
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> paymentService.processPayment(requestInvalidAmount));

        verify(paymentRepository, never()).findByIdempotencyKey(anyString());
        verify(externalSystemMock, never()).sendPayment(any(Payment.class));
        verify(paymentRepository, never()).save(any(Payment.class));

        // Case 3. - amount is 0
        requestInvalidAmount.setAmount(BigDecimal.ZERO.intValue());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> paymentService.processPayment(requestInvalidAmount));

        verify(paymentRepository, never()).findByIdempotencyKey(anyString());
        verify(externalSystemMock, never()).sendPayment(any(Payment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
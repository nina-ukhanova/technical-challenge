package com.lp.payment.integration;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.external.ExternalSystemMock;
import com.lp.payment.repository.PaymentRepository;
import com.lp.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoSpyBean
    private ExternalSystemMock externalSystemMock;

    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    void clear() {
        paymentRepository.deleteAll();
    }

    @Test
    void testTransactionRollback_WhenExternalSystemFails() {

        PaymentRequest request = new PaymentRequest();
        request.setCardHolder("Test User");
        request.setAmount(100);
        request.setCurrency("USD");
        request.setCardNumber("1234567890123456");

        doThrow(new RuntimeException("Fails to execute request"))
                .when(externalSystemMock).sendPayment(any());

        long initialCount = paymentRepository.count();

        try {
            paymentService.processPayment(request);
            fail("Expected RuntimeException to be thrown");
        } catch (RuntimeException e) {
            // Expected exception
            assertEquals("Fails to execute request", e.getMessage());
        }

        long countAfterProcessing = paymentRepository.count();

        assertEquals(initialCount, countAfterProcessing, "Payment should be rolled back when external system fails");
    }

    @Test
    void testTransactionCommit_WhenExternalSystemSucceeds() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setCardHolder("Test User");
        request.setAmount(200);
        request.setCurrency("EUR");
        request.setCardNumber("9876543210987654");

        long initialCount = paymentRepository.count();

        Payment result = paymentService.processPayment(request);

        long countAfterProcessing = paymentRepository.count();

        assertEquals( initialCount + 1, countAfterProcessing, "Payment should be committed when external system succeeds");
        assertEquals("AUTHORISED", result.getStatus());

        Payment savedPayment = paymentRepository.findAll().get(0);
        assertEquals("AUTHORISED", savedPayment.getStatus());
        assertEquals("Test User", savedPayment.getCardHolder());
    }

}

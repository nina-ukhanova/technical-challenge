package com.lp.payment.integration;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.external.ExternalSystemMock;
import com.lp.payment.repository.PaymentRepository;
import com.lp.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Slf4j
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
    void testProcessPaymentTransactionRollback_WhenExternalSystemFails() {

        PaymentRequest request = new PaymentRequest();
        request.setCardHolder("Test User");
        request.setAmount(BigDecimal.valueOf(100));
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
    void testProcessPaymentTransactionCommit_WhenExternalSystemSucceeds() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setCardHolder("Test User");
        request.setAmount(BigDecimal.valueOf(200));
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

    @Test
    void testUUID_NoCollisionsWithConcurrentCreation() throws InterruptedException {

        int numberOfThreads = 10;
        int paymentsPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        try (ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {

            Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = new CopyOnWriteArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < paymentsPerThread; j++) {
                            PaymentRequest request = new PaymentRequest();
                            request.setCardHolder("User-" + threadId + "-" + j);
                            request.setAmount(BigDecimal.valueOf(100 + j));
                            request.setCurrency("USD");
                            request.setCardNumber("1234567890123456");

                            Payment payment = paymentService.processPayment(request);
                            generatedIds.add(payment.getId());
                        }
                    } catch (Exception e) {
                        log.error("Process payment fails {}", e.getMessage());
                        exceptions.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "All threads should complete within timeout");
            assertTrue(exceptions.isEmpty(),
                    "No exceptions should occur. Found: " + exceptions.stream()
                            .map(Exception::getMessage)
                            .collect(Collectors.joining(", ")));

            int expectedPayments = numberOfThreads * paymentsPerThread;
            assertEquals(expectedPayments, generatedIds.size(),
                    "All generated IDs should be unique (no collisions)");

            long actualPayments = paymentRepository.count();
            assertEquals(expectedPayments, actualPayments,
                    "All payments should be persisted");
        }
    }
}

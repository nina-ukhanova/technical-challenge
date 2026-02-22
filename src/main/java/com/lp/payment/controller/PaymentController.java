package com.lp.payment.controller;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.repository.PaymentRepository;
import com.lp.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    private final PaymentRepository repository;

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) throws InterruptedException {
        log.info("Received payment request - Amount: {}, Currency: {}, MaskedCard: ****{}",
                request.getAmount(), request.getCurrency(),
                request.getCardNumber().substring(request.getCardNumber().length() - 4));

        try {
            Payment payment = paymentService.processPayment(request);
            log.info("Payment request completed - PaymentId: {}, Status: {}", payment.getId(), payment.getStatus());
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            log.warn("Payment validation failed - Error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Payment processing failed - Amount: {}, Currency: {}, Error: {}",
                    request.getAmount(), request.getCurrency(), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    public int countPayments() {
        int count = repository.findAll().size();
        log.debug("Payment count requested - Total: {}", count);
        return count;
    }
}

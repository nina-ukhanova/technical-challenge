package com.lp.payment.controller;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.repository.PaymentRepository;
import com.lp.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private final PaymentRepository repository;

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) throws InterruptedException {
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.ok(payment);
    }

    @GetMapping
    public int countPayments() {
        return repository.findAll().size();
    }
}

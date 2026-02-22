package com.lp.payment.controller;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.repository.PaymentRepository;
import com.lp.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository repository;

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

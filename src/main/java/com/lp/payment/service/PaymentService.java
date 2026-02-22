package com.lp.payment.service;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.external.ExternalSystemMock;
import com.lp.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final ExternalSystemMock externalSystem;
    private final DateTimeFormatter formatter;

    public PaymentService(PaymentRepository paymentRepository, ExternalSystemMock externalSystemMock) {
        formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssnnnnnnnnn");

        repository = paymentRepository;
        externalSystem = externalSystemMock;
    }

    @Transactional
    public Payment processPayment(PaymentRequest request) {

        final int requestAmount = request.getAmount();
        if (requestAmount <= 0 || requestAmount > 199999999) {
            throw new IllegalArgumentException("Amount in the request must be greater 0 and less or equal 199999999");
        }

        var idempotencyKey = generateIdempotencyKey(request.getCardHolder(), request.getAmount(), request.getCurrency(), request.getCardNumber());

        Optional<Payment> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setCardHolder(request.getCardHolder());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMaskedCard(request.getCardNumber().substring(request.getCardNumber().length() - 4));
        payment.setIdempotencyKey(idempotencyKey);

        repository.save(payment);

        var response = externalSystem.sendPayment(payment); // TODO handle errors - blocking request

        repository.save(response);
        return response;
    }

    public String generateIdempotencyKey(String cardHolderName,
                                         float amount,
                                         String currency,
                                         String cardNumber) {

        String timePart = LocalDateTime.now().format(formatter);

        String data = cardHolderName + "|" + amount + "|" + currency + "|" + cardNumber + "|" + timePart;

        return sha256Base64(data);
    }

    private String sha256Base64(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}

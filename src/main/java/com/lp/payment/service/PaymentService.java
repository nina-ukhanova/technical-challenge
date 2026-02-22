package com.lp.payment.service;

import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.external.ExternalSystemMock;
import com.lp.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class PaymentService {
    private final SimpleDateFormat sdf;
    private final MessageDigest digest;

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private ExternalSystemMock externalSystem;

    public PaymentService() throws NoSuchAlgorithmException {
        sdf = new SimpleDateFormat("yyyyMMddHHmm");
        digest = MessageDigest.getInstance("SHA-256");
    }

    public Payment processPayment(PaymentRequest request) {

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

        var response = externalSystem.sendPayment(payment);

        repository.save(response);
        return response;
    }

    public String generateIdempotencyKey(String cardHolderName,
                                         float amount,
                                         String currency,
                                         String cardNumber) {

        String timePart = sdf.format(new Date());

        String data = cardHolderName + "|" + amount + "|" + currency + "|" + cardNumber + "|" + timePart;

        return sha256Base64(data);
    }

    private String sha256Base64(String data) {
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}

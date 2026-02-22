package com.lp.payment.service;

import com.lp.payment.common.exception.PaymentProcessingException;
import com.lp.payment.dto.PaymentRequest;
import com.lp.payment.entity.Payment;
import com.lp.payment.external.ExternalSystemMock;
import com.lp.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
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

        final String maskedCard = request.getCardNumber().substring(request.getCardNumber().length() - 4);
        log.info("Processing payment request - Amount: {}, Currency: {}, MaskedCard: ****{}",
                request.getAmount(), request.getCurrency(), maskedCard);

        final BigDecimal requestAmount = request.getAmount();
        if (requestAmount.compareTo(BigDecimal.ZERO) <= 0 || requestAmount.compareTo(BigDecimal.valueOf(199999999)) > 0) {
            log.error("Invalid payment amount: {} - must be greater than 0 and less or equal to 199999999", requestAmount);
            throw new IllegalArgumentException("Amount in the request must be greater 0 and less or equal 199999999");
        }

        var idempotencyKey = generateIdempotencyKey(request.getCardHolder(), request.getAmount(), request.getCurrency(), request.getCardNumber());
        log.debug("Generated idempotency key: {}", idempotencyKey);

        Optional<Payment> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment existingPayment = existing.get();
            log.info("Payment already exists (idempotency key matched) - PaymentId: {}, Status: {}, Amount: {}, Currency: {}",
                    existingPayment.getId(), existingPayment.getStatus(), 
                    existingPayment.getAmount(), existingPayment.getCurrency());
            return existingPayment;
        }

        log.debug("No existing payment found, creating new payment record");

        Payment payment = new Payment();
        payment.setCardHolder(request.getCardHolder());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMaskedCard(maskedCard);
        payment.setIdempotencyKey(idempotencyKey);

        repository.save(payment);
        log.debug("Payment saved to database - PaymentId: {}, Status: {}", payment.getId(), payment.getStatus());

        log.info("Sending payment to external system - PaymentId: {}, Amount: {}, Currency: {}, MaskedCard: ****{}",
                payment.getId(), payment.getAmount(), payment.getCurrency(), payment.getMaskedCard());

        Payment response;
        try {
            response = externalSystem.sendPayment(payment);
            log.info("External system response received - PaymentId: {}, Status: {}", response.getId(), response.getStatus());

        } catch (Exception exception) {
            throw new PaymentProcessingException(
                    "Failed to process payment with external system: " + exception.getMessage(),
                    payment.getId().toString(),
                    exception);
        }

        repository.save(response);
        log.info("Payment processing completed successfully - PaymentId: {}, FinalStatus: {}, Amount: {}, Currency: {}",
                response.getId(), response.getStatus(), response.getAmount(), response.getCurrency());
        return response;
    }

    public String generateIdempotencyKey(String cardHolderName,
                                         BigDecimal amount,
                                         String currency,
                                         String cardNumber) {

        log.debug("Generating idempotency key - Amount: {}, Currency: {}", amount, currency);

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

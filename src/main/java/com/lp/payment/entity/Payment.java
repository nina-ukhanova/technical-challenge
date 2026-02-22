package com.lp.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String cardHolder;

    @Column(precision = 13, scale = 4)
    private BigDecimal amount;
    private String currency;
    private String maskedCard;

    @Column(unique = true)
    private String idempotencyKey;

    private Instant createdAt = Instant.now();

    private String status = "PENDING";
}

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
import java.util.UUID;

@Data
@Entity
@Table(name = "payments") //TODO the name of the table can be discussed to use single
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String cardHolder;

    @Column(precision = 13, scale = 4)
    private BigDecimal amount;
    private String currency; // TODO use enum
    private String maskedCard;

    @Column(unique = true)
    private String idempotencyKey;

    private Instant createdAt = Instant.now();

    private String status = "PENDING";
}

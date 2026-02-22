package com.lp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private Long id = new Date().getTime();

    private String cardHolder;
    private float amount;
    private String currency;
    private String maskedCard;

    @Column
    private String idempotencyKey;

    private Instant createdAt = Instant.now();

    private String status = "PENDING";
}

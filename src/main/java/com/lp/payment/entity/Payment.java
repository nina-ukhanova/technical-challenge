package com.lp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

@Data
@Entity
@Table(name = "payments") //TODO the name of the table can be discussed to use single
public class Payment {

    @Id
    private Long id = new Date().getTime();

    //TODO it is better to update the name of the columns in DB to use snake case
    private String cardHolder;
    private float amount;
    private String currency; // TODO use enum
    private String maskedCard;

    @Column // TODO why this annotation is used without defining a name?
    private String idempotencyKey;

    private Instant createdAt = Instant.now();

    private String status = "PENDING";
}

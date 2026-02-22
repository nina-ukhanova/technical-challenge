package com.lp.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String cardHolder;
    private BigDecimal amount;
    private String currency;
    private String cardNumber;
}
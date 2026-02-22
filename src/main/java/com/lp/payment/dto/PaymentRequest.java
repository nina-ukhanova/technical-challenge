package com.lp.payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String cardHolder;
    private int amount;
    private String currency;
    private String cardNumber;
}
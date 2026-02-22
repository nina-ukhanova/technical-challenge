package com.lp.payment.common.exception;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {

    private final String paymentId;

    public PaymentProcessingException(String message, String paymentId, Throwable cause) {
        super(message, cause);
        this.paymentId = paymentId;
    }
}


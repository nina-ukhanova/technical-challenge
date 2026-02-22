package com.lp.payment.external;

import com.lp.payment.entity.Payment;
import org.springframework.stereotype.Service;

@Service
public class ExternalSystemMock {

    public Payment sendPayment(Payment payment) {
        var response = new Payment();
        response.setId(payment.getId());
        response.setCurrency(payment.getCurrency());
        response.setAmount(payment.getAmount());
        response.setCardHolder(payment.getCardHolder());
        response.setIdempotencyKey(payment.getIdempotencyKey());
        response.setCreatedAt(payment.getCreatedAt());
        payment.setStatus("AUTHORISED");
        return payment;
    }
}

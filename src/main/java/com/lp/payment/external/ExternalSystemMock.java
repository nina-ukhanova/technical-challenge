package com.lp.payment.external;

import com.lp.payment.entity.Payment;
import org.springframework.stereotype.Service;

@Service
public class ExternalSystemMock {

    public Payment sendPayment(Payment payment) {
        payment.setStatus("AUTHORISED");
        return payment;
    }
}

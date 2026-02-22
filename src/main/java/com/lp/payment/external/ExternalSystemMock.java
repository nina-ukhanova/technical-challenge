package com.lp.payment.external;

import com.lp.payment.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExternalSystemMock {

    public Payment sendPayment(Payment payment) {
        log.info("External payment processing started - PaymentId: {}, Amount: {}, Currency: {}, MaskedCard: ****{}",
                payment.getId(), payment.getAmount(), payment.getCurrency(), payment.getMaskedCard());
        
        payment.setStatus("AUTHORISED");
        
        log.info("External payment processing completed - PaymentId: {}, Status: {}", 
                payment.getId(), payment.getStatus());
        
        return payment;
    }
}

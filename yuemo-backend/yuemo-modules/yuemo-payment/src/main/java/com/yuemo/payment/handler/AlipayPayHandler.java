package com.yuemo.payment.handler;

import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlipayPayHandler implements PayTypeHandler {

    @Override
    public int payType() {
        return 2;
    }

    @Override
    public Payment doPay(Long userId, Payment payment) {
        payment.setStatus(PaymentStatus.PENDING.getCode());
        log.info("支付宝预下单: userId={}, orderNo={}, amount={}", userId, payment.getOrderNo(), payment.getAmount());
        return payment;
    }
}

package com.yuemo.payment.handler;

import com.yuemo.payment.entity.Payment;

public interface PayTypeHandler {

    int payType();

    Payment doPay(Long userId, Payment payment);
}

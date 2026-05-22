package com.yuemo.payment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.payment.entity.Payment;

import java.util.Map;

public interface PaymentService {

    Payment createPayment(Long userId, Long orderId, Integer payType);

    String handleCallback(Integer payType, Map<String, String> params);

    Payment getPaymentByIdAndUserId(Long id, Long userId);

    IPage<Payment> listPayments(Long userId, Integer page, Integer size);

    void refund(Long userId, Long orderId, String reason);
}

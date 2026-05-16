package com.yuemo.payment.service;

import com.yuemo.payment.entity.Payment;

import java.util.Map;

public interface PaymentService {

    Payment createPayment(Long userId, Long orderId);

    String handleCallback(Integer payType, Map<String, String> params);

    Payment getPaymentById(Long id);

    void refund(Long userId, Long orderId);
}

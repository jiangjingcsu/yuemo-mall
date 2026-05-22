package com.yuemo.payment.vo;

import com.yuemo.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentVO(
    Long id,
    String paymentNo,
    Long orderId,
    String orderNo,
    Long userId,
    BigDecimal amount,
    Integer payType,
    Integer status,
    LocalDateTime payTime,
    LocalDateTime createTime
) {

    public static PaymentVO from(Payment payment) {
        return new PaymentVO(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getOrderNo(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPayType(),
                payment.getStatus(),
                payment.getPayTime(),
                payment.getCreateTime()
        );
    }
}

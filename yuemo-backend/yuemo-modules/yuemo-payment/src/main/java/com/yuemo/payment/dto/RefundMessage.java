package com.yuemo.payment.dto;

public record RefundMessage(
        Long orderId,
        Long userId,
        java.math.BigDecimal amount
) {}

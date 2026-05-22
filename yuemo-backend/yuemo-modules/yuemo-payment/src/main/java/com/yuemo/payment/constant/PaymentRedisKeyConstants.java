package com.yuemo.payment.constant;

public final class PaymentRedisKeyConstants {

    private PaymentRedisKeyConstants() {
    }

    public static final String PAYMENT_CALLBACK_PREFIX = "payment:callback:";
    public static final long CALLBACK_TTL_MINUTES = 5;
}

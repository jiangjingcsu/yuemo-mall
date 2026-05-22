package com.yuemo.promotion.constant;

public final class PromotionRedisKeyConstants {

    private PromotionRedisKeyConstants() {
    }

    public static final String COUPON_RECEIVE_PREFIX = "coupon:receive:";
    public static final long COUPON_RECEIVE_TTL_DAYS = 7;
}

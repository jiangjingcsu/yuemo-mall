package com.yuemo.promotion.enums;

import lombok.Getter;

@Getter
public enum CouponType {

    THRESHOLD_DISCOUNT(1, "满减"),
    DISCOUNT(2, "折扣"),
    FIXED_DISCOUNT(3, "立减");

    private final int code;
    private final String desc;

    CouponType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponType fromCode(int code) {
        for (CouponType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("未知优惠券类型: " + code);
    }
}

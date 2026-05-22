package com.yuemo.promotion.enums;

import lombok.Getter;

@Getter
public enum CouponStatus {

    NOT_STARTED(0, "未开始"),
    ACTIVE(1, "进行中"),
    ENDED(2, "已结束");

    private final int code;
    private final String desc;

    CouponStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponStatus fromCode(int code) {
        for (CouponStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知优惠券状态: " + code);
    }
}

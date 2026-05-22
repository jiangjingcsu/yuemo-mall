package com.yuemo.promotion.enums;

import lombok.Getter;

@Getter
public enum UserCouponStatus {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final int code;
    private final String desc;

    UserCouponStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserCouponStatus fromCode(int code) {
        for (UserCouponStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知用户券状态: " + code);
    }
}

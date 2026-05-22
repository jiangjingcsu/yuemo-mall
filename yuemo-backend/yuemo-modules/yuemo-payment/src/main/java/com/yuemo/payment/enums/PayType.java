package com.yuemo.payment.enums;

import lombok.Getter;

@Getter
public enum PayType {

    WECHAT(1, "微信支付"),
    ALIPAY(2, "支付宝"),
    BALANCE(3, "平台余额");

    private final int code;
    private final String desc;

    PayType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PayType fromCode(int code) {
        for (PayType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("未知支付方式: " + code);
    }
}

package com.yuemo.payment.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    PENDING(0, "待支付"),
    SUCCESS(1, "支付成功"),
    FAILED(2, "支付失败"),
    REFUNDED(3, "已退款");

    private final int code;
    private final String desc;

    PaymentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PaymentStatus fromCode(int code) {
        for (PaymentStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知支付状态: " + code);
    }
}

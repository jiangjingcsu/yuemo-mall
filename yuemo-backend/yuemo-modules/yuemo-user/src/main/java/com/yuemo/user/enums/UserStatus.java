package com.yuemo.user.enums;

import lombok.Getter;

@Getter
public enum UserStatus {

    NORMAL(0, "正常"),
    DISABLED(1, "禁用");

    private final int code;
    private final String desc;

    UserStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知用户状态: " + code);
    }
}

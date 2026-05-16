package com.yuemo.common.core.utils;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码加密工具 — 基于 Hutool BCrypt
 */
public class PasswordEncoder {

    private PasswordEncoder() {}

    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}

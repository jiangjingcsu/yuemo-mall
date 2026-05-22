package com.yuemo.common.security.constant;

public final class AuthRedisKeyConstants {

    private AuthRedisKeyConstants() {
    }

    public static final String TOKEN_USER_PREFIX = "token:user:";
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    public static final String USER_ROLE_PREFIX = "user:role:";

    public static final long TOKEN_TTL_MINUTES = 30;
    public static final long ROLE_TTL_HOURS = 24;
}

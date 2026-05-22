package com.yuemo.user.vo;

public record LoginVO(
    String accessToken,
    String refreshToken,
    Long userId,
    String username
) {}

package com.yuemo.user.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
}

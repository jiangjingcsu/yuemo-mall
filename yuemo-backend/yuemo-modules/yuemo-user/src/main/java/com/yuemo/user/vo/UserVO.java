package com.yuemo.user.vo;

import com.yuemo.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserVO(
    Long id,
    String username,
    String nickname,
    String phone,
    String email,
    String avatar,
    Integer gender,
    Integer status,
    BigDecimal balance,
    LocalDateTime createTime
) {

    public static UserVO from(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatar(),
                user.getGender(),
                user.getStatus(),
                user.getBalance(),
                user.getCreateTime()
        );
    }
}

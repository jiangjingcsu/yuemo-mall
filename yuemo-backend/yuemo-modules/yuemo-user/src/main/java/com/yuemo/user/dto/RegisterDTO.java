package com.yuemo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
    @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 32, message = "用户名长度须在 3-32 之间") String username,
    @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码长度须在 6-64 之间") String password,
    String nickname,
    String phone
) {}

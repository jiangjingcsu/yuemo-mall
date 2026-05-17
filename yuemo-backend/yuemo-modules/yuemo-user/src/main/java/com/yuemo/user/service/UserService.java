package com.yuemo.user.service;

import com.yuemo.user.dto.LoginDTO;
import com.yuemo.user.dto.RegisterDTO;
import com.yuemo.user.entity.User;
import com.yuemo.user.vo.LoginVO;

import java.math.BigDecimal;

public interface UserService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    void logout(Long userId);

    User getUserById(Long id);

    void updateUser(Long id, User user);

    BigDecimal getBalance(Long userId);

    void deductBalance(Long userId, BigDecimal amount);
}

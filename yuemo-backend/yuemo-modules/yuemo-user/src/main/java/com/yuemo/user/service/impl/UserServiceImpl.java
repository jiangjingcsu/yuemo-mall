package com.yuemo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.utils.PasswordEncoder;
import com.yuemo.common.security.utils.JwtTokenProvider;
import com.yuemo.user.dto.LoginDTO;
import com.yuemo.user.dto.RegisterDTO;
import com.yuemo.user.entity.User;
import com.yuemo.user.mapper.UserMapper;
import com.yuemo.user.service.UserService;
import com.yuemo.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(RegisterDTO dto) {
        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (exists) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setStatus(0);
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!PasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 缓存 Token
        String cacheKey = "token:user:" + user.getId();
        redisTemplate.opsForValue().set(cacheKey, accessToken, 30, TimeUnit.MINUTES);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        return vo;
    }

    @Override
    public void logout(Long userId) {
        String cacheKey = "token:user:" + userId;
        String token = (String) redisTemplate.opsForValue().get(cacheKey);
        if (token != null) {
            // 加入黑名单
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1", 30, TimeUnit.MINUTES);
        }
        redisTemplate.delete(cacheKey);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public void updateUser(Long id, User user) {
        user.setId(id);
        userMapper.updateById(user);
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = getUserById(userId);
        return user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, BigDecimal amount) {
        User user = getUserById(userId);
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), "账户余额不足");
        }
        user.setBalance(balance.subtract(amount));
        userMapper.updateById(user);
    }
}

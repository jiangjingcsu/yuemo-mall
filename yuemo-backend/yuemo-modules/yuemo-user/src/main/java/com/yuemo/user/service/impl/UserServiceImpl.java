package com.yuemo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.utils.PasswordEncoder;
import com.yuemo.common.security.constant.AuthRedisKeyConstants;
import com.yuemo.common.security.utils.JwtTokenProvider;
import com.yuemo.user.dto.LoginDTO;
import com.yuemo.user.dto.RegisterDTO;
import com.yuemo.user.entity.User;
import com.yuemo.user.entity.UserRole;
import com.yuemo.user.mapper.UserMapper;
import com.yuemo.user.mapper.UserRoleMapper;
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
    private final UserRoleMapper userRoleMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(RegisterDTO dto) {
        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.username()));
        if (exists) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(PasswordEncoder.encode(dto.password()));
        user.setNickname(dto.nickname());
        user.setPhone(dto.phone());
        user.setStatus(0);
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole("USER");
        userRoleMapper.insert(userRole);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.username()));
        if (user == null || !PasswordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_LOGIN_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        String tokenKey = AuthRedisKeyConstants.TOKEN_USER_PREFIX + user.getId();
        redisTemplate.opsForValue().set(tokenKey, accessToken,
                AuthRedisKeyConstants.TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        UserRole userRole = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        String role = userRole != null ? userRole.getRole() : "USER";
        String roleKey = AuthRedisKeyConstants.USER_ROLE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(roleKey, role,
                AuthRedisKeyConstants.ROLE_TTL_HOURS, TimeUnit.HOURS);

        return new LoginVO(accessToken, refreshToken, user.getId(), user.getUsername());
    }

    @Override
    public void logout(Long userId) {
        String tokenKey = AuthRedisKeyConstants.TOKEN_USER_PREFIX + userId;
        String token = (String) redisTemplate.opsForValue().get(tokenKey);
        if (token != null) {
            String blacklistKey = AuthRedisKeyConstants.TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(blacklistKey, "1",
                    AuthRedisKeyConstants.TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        }
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(AuthRedisKeyConstants.USER_ROLE_PREFIX + userId);
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
    public void updateUser(Long id, Long currentUserId, User user) {
        if (!id.equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        user.setId(id);
        user.setPassword(null);
        user.setBalance(null);
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
        int updated = userMapper.deductBalance(userId, amount);
        if (updated == 0) {
            throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(), "账户余额不足");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBalance(Long userId, BigDecimal amount) {
        int updated = userMapper.addBalance(userId, amount);
        if (updated == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
    }
}

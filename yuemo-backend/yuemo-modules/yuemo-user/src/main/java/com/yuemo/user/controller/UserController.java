package com.yuemo.user.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.user.dto.LoginDTO;
import com.yuemo.user.dto.RegisterDTO;
import com.yuemo.user.entity.User;
import com.yuemo.user.service.UserService;
import com.yuemo.user.vo.AddressVO;
import com.yuemo.user.vo.LoginVO;
import com.yuemo.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute("userId") Long userId) {
        userService.logout(userId);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserVO> info(@RequestAttribute("userId") Long userId) {
        return Result.success(UserVO.from(userService.getUserById(userId)));
    }

    @PutMapping("/info")
    public Result<Void> updateInfo(@RequestAttribute("userId") Long userId, @RequestBody User user) {
        userService.updateUser(userId, userId, user);
        return Result.success();
    }

    @GetMapping("/balance")
    public Result<BigDecimal> balance(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getBalance(userId));
    }
}

package com.yuemo.user.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.user.dto.LoginDTO;
import com.yuemo.user.dto.RegisterDTO;
import com.yuemo.user.entity.User;
import com.yuemo.user.service.UserService;
import com.yuemo.user.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<User> info(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getUserById(userId));
    }

    @PutMapping("/info")
    public Result<Void> updateInfo(@RequestAttribute("userId") Long userId, @RequestBody User user) {
        userService.updateUser(userId, user);
        return Result.success();
    }

    @GetMapping("/balance")
    public Result<java.math.BigDecimal> balance(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getBalance(userId));
    }
}

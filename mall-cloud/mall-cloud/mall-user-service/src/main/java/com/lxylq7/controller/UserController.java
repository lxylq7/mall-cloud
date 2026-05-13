package com.lxylq7.controller;

import com.lxylq7.auth.CurrentUserContext;
import com.lxylq7.common.Result;
import com.lxylq7.dto.UserDTO;
import com.lxylq7.dto.UserLoginRequest;
import com.lxylq7.dto.UserLoginResponse;
import com.lxylq7.dto.UserRegisterRequest;
import com.lxylq7.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody UserRegisterRequest req) {
        UserDTO user = authService.register(req);
        return Result.ok("注册成功", user);
    }

    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest req) {
        UserLoginResponse resp = authService.login(req.getUsername(), req.getPassword());
        return Result.ok("登录成功", resp);
    }

    @GetMapping("/profile")
    public Result<UserDTO> profile() {
        CurrentUserContext.CurrentUser cu = CurrentUserContext.get();
        if (cu == null) {
            return Result.fail(401, "未登录");
        }
        UserDTO user = authService.getById(cu.getUserId());
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.ok(user);
    }

    @PostMapping("/logout")
    public Result<Object> logout() {
        return Result.ok("已退出", null);
    }

    @GetMapping("/users/{userId}")
    public Result<UserDTO> getById(@PathVariable Long userId) {
        UserDTO user = authService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.ok(user);
    }
}

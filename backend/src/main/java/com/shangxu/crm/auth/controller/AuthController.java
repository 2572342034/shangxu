package com.shangxu.crm.auth.controller;

import com.shangxu.crm.auth.dto.LoginRequest;
import com.shangxu.crm.auth.dto.LoginResponse;
import com.shangxu.crm.auth.service.AuthService;
import com.shangxu.crm.common.R;
import com.shangxu.crm.security.LoginUser;
import com.shangxu.crm.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok("登录成功", authService.login(request));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public R<LoginUser> info() {
        LoginUser loginUser = authService.info();
        loginUser.setPassword(null);
        return R.ok(loginUser);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok("已退出登录", null);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok("密码修改成功，请重新登录", null);
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody Map<String, String> body) {
        authService.updateProfile(body.get("nickname"), body.get("mobile"), body.get("email"), body.get("avatar"));
        return R.ok();
    }

    @Operation(summary = "当前用户简要信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(Map.of(
                "id", SecurityUtils.getUserId() == null ? 0 : SecurityUtils.getUserId(),
                "nickname", SecurityUtils.getNickname() == null ? "" : SecurityUtils.getNickname()));
    }
}

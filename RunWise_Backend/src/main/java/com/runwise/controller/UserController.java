package com.runwise.controller;

import com.runwise.common.Result;
import com.runwise.entity.User;
import com.runwise.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料")
    public Result<User> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getById(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<Void> updateProfile(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("userId");
        user.setId(userId);
        userService.updateById(user);
        return Result.success();
    }

    @GetMapping("/agreement-status")
    @Operation(summary = "查询协议同意状态")
    public Result<Map<String, Object>> getAgreementStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        String currentVersion = "1.0.0";
        boolean agreed = user.getAgreementVersion() != null
                && user.getAgreementVersion().equals(currentVersion);
        return Result.success(Map.of(
                "agreed", agreed,
                "currentVersion", currentVersion,
                "agreedVersion", user.getAgreementVersion(),
                "agreedTime", user.getAgreementTime() != null ? user.getAgreementTime().toString() : null
        ));
    }

    @PostMapping("/agree-agreement")
    @Operation(summary = "同意用户协议与隐私政策")
    public Result<Void> agreeAgreement(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String version = body.getOrDefault("version", "1.0.0");
        User user = new User();
        user.setId(userId);
        user.setAgreementVersion(version);
        user.setAgreementTime(LocalDateTime.now());
        userService.updateById(user);
        return Result.success();
    }
}
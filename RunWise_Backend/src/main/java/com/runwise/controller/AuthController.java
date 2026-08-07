package com.runwise.controller;

import com.runwise.common.Result;
import com.runwise.dto.LoginDTO;
import com.runwise.service.UserService;
import com.runwise.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "微信登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新token")
    public Result<LoginVO> refresh(@RequestHeader("Authorization") String authHeader) {
        // 从 Header 中提取 refresh_token
        String refreshToken = authHeader.replace("Bearer ", "").trim();
        return Result.success(userService.refreshToken(refreshToken));
    }
}

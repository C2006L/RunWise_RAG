package com.runwise.controller;

import com.runwise.common.Result;
import com.runwise.dto.LoginDTO;
import com.runwise.service.UserService;
import com.runwise.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import com.runwise.dto.PasswordLoginDTO;
import com.runwise.service.PasswordAuthService;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口")
public class AuthController {

    private final UserService userService;
    private final PasswordAuthService passwordAuth;

    public AuthController(UserService userService, PasswordAuthService passwordAuth) {
        this.userService = userService;
        this.passwordAuth = passwordAuth;
    }

    @PostMapping("/login")
    @Operation(summary = "微信登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新token")
    public Result<LoginVO> refresh(@RequestHeader(value="Authorization",required=false) String authHeader) {
        if (authHeader==null || !authHeader.startsWith("Bearer ")) throw new com.runwise.common.BusinessException(com.runwise.common.ResultCode.UNAUTHORIZED);
        // 从 Header 中提取 refresh_token
        String refreshToken = authHeader.replace("Bearer ", "").trim();
        return Result.success(userService.refreshToken(refreshToken));
    }

    @PostMapping("/login-password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginDTO input) {
        return Result.success(passwordAuth.login(input));
    }
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody PasswordLoginDTO input) {
        return Result.success(passwordAuth.register(input));
    }
    @PostMapping("/bind-password")
    public Result<Void> bind(@Valid @RequestBody PasswordLoginDTO input,HttpServletRequest request) {
        passwordAuth.bind((Long)request.getAttribute("userId"),input);return Result.success();
    }
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        passwordAuth.logout((Long)request.getAttribute("userId"),request.getHeader("Authorization").substring(7).trim());return Result.success();
    }
    @GetMapping("/account")
    public Result<java.util.Map<String,Object>> account(@RequestAttribute("userId") Long user){return Result.success(passwordAuth.account(user));}
    @PostMapping("/bind-wechat")
    public Result<Void> bindWechat(@RequestAttribute("userId") Long user,@Valid @RequestBody LoginDTO input){userService.bindWechat(user,input.getCode());return Result.success();}
}

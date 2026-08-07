package com.runwise.interceptor;

import com.runwise.common.BusinessException;
import com.runwise.common.JwtUtils;
import com.runwise.common.ResultCode;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 * 校验请求头中的 access_token，并将 userId 存入 request 属性
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    public JwtInterceptor(JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从 Header 获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();

        // 2. 校验 token 格式与有效期
        if (!jwtUtils.isValid(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "token已过期");
        }

        // 3. 解析 userId
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        // 4. 校验 Redis 中是否仍存在（支持主动登出/黑名单）
        String redisKey = "runwise:token:" + userId + ":access";
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }

        // 5. 将 userId 放入 request 供 Controller 使用
        request.setAttribute("userId", userId);
        log.debug("认证通过, userId={}", userId);
        return true;
    }
}

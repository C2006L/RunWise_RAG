package com.runwise.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

/**
 * JWT 工具类
 * 负责 Token 的生成、解析、校验
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${runwise.jwt.secret}")
    private String secret;

    @Value("${runwise.jwt.access-token-expire}")
    private Long accessTokenExpire;

    @Value("${runwise.jwt.refresh-token-expire}")
    private Long refreshTokenExpire;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 access_token (2小时)
     */
    public String generateAccessToken(Long userId, String openid) {
        return generateToken(userId, openid, accessTokenExpire, "access");
    }

    /**
     * 生成 refresh_token (7天)
     */
    public String generateRefreshToken(Long userId, String openid) {
        return generateToken(userId, openid, refreshTokenExpire, "refresh");
    }

    private String generateToken(Long userId, String openid, Long expireMs, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("openid", openid);
        claims.put("type", type);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 token 中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 校验 token 是否有效
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("token校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 access_token 过期时间(毫秒)
     */
    public Long getAccessTokenExpire() {
        return accessTokenExpire;
    }

    /**
     * 获取 refresh_token 过期时间(毫秒)
     */
    public Long getRefreshTokenExpire() {
        return refreshTokenExpire;
    }
}

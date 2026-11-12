package com.runwise.admin;

import com.runwise.common.*;
import io.jsonwebtoken.Claims;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AdminAuthService {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final JwtUtils jwt;
    public AdminAuthService(JdbcTemplate jdbc, StringRedisTemplate redis, JwtUtils jwt) {
        this.jdbc = jdbc; this.redis = redis; this.jwt = jwt;
    }
    public Map<String, Object> login(String username, String password) {
        username = username.toLowerCase(Locale.ROOT);
        String attempts = "runwise:admin:login:" + username;
        String count = redis.opsForValue().get(attempts);
        if (count != null && Integer.parseInt(count) >= 5)
            throw new BusinessException(ResultCode.FORBIDDEN, "尝试次数过多，请15分钟后重试");
        List<AdminAccount> accounts = jdbc.query("SELECT * FROM t_admin_account WHERE username=?",
                new BeanPropertyRowMapper<>(AdminAccount.class), username);
        // 未知账户仍计算密码哈希，避免快速返回泄漏账户是否存在。
        boolean valid = accounts.isEmpty() ? AdminPassword.matches(password, DUMMY_HASH)
                : AdminPassword.matches(password, accounts.get(0).getPasswordHash());
        if (!valid || accounts.isEmpty() || !Integer.valueOf(1).equals(accounts.get(0).getEnabled())) {
            redis.opsForValue().increment(attempts);
            redis.expire(attempts, 15, TimeUnit.MINUTES);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号或密码错误，或账号已停用");
        }
        AdminAccount account = accounts.get(0);
        if (!Arrays.asList("SUPER_ADMIN", "CONTENT_EDITOR").contains(account.getRole()))
            throw new BusinessException(ResultCode.FORBIDDEN);
        String token = jwt.generateAdminToken(account.getId());
        redis.opsForValue().set(sessionKey(account.getId()), token, jwt.getAccessTokenExpire(), TimeUnit.MILLISECONDS);
        redis.delete(attempts);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token); result.put("account", account);
        return result;
    }
    private static final String DUMMY_HASH = AdminPassword.hash("not-a-real-account-password");
    public AdminAccount authenticate(String header) {
        try {
            if (header == null || !header.startsWith("Bearer ")) throw new IllegalArgumentException();
            String token = header.substring(7).trim();
            Claims claims = jwt.parseToken(token);
            if (!"admin".equals(claims.get("scope"))) throw new IllegalArgumentException();
            Long id = Long.valueOf(claims.getSubject());
            if (!token.equals(redis.opsForValue().get(sessionKey(id)))) throw new IllegalArgumentException();
            List<AdminAccount> accounts = jdbc.query("SELECT * FROM t_admin_account WHERE id=? AND enabled=1",
                    new BeanPropertyRowMapper<>(AdminAccount.class), id);
            if (accounts.isEmpty()) throw new IllegalArgumentException();
            AdminAccount account = accounts.get(0);
            if (!Arrays.asList("SUPER_ADMIN", "CONTENT_EDITOR").contains(account.getRole()))
                throw new IllegalArgumentException();
            return account;
        } catch (IllegalArgumentException | io.jsonwebtoken.JwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "管理员登录已失效");
        }
    }
    public void logout(AdminAccount account) { redis.delete(sessionKey(account.getId())); }
    private String sessionKey(Long id) { return "runwise:admin:session:" + id; }
}

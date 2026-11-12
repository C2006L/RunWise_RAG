package com.runwise.service;

import com.runwise.common.*;
import com.runwise.dto.PasswordLoginDTO;
import com.runwise.entity.User;
import com.runwise.vo.LoginVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordAuthService {
    private final JdbcTemplate jdbc;
    private final UserService users;
    private final JwtUtils jwt;
    private final StringRedisTemplate redis;
    private static final String DUMMY_HASH = PasswordHasher.hash("dummy-password-not-a-real-account");
    public PasswordAuthService(JdbcTemplate jdbc,UserService users,JwtUtils jwt,StringRedisTemplate redis) {
        this.jdbc=jdbc;this.users=users;this.jwt=jwt;this.redis=redis;
    }
    public LoginVO login(PasswordLoginDTO input) {
        String name=input.getUsername().toLowerCase(Locale.ROOT);
        String key="runwise:web:login:"+name;
        String attempts=redis.opsForValue().get(key);
        if(attempts!=null && Integer.parseInt(attempts)>=5)
            throw new BusinessException(ResultCode.FORBIDDEN,"尝试次数过多，请15分钟后重试");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT user_id,password_hash FROM t_web_credential WHERE username=?",name);
        String hash=rows.isEmpty()?DUMMY_HASH:(String)rows.get(0).get("password_hash");
        boolean valid=PasswordHasher.matches(input.getPassword(),hash);
        User user=rows.isEmpty()?null:users.getById(((Number)rows.get(0).get("user_id")).longValue());
        if(!valid || user==null || !Integer.valueOf(1).equals(user.getStatus())) {
            redis.opsForValue().increment(key);redis.expire(key,15,TimeUnit.MINUTES);
            throw new BusinessException(ResultCode.UNAUTHORIZED,"账号或密码错误，或账户不可用");
        }
        redis.delete(key);return issue(user,false);
    }
    @Transactional
    public LoginVO register(PasswordLoginDTO input) {
        User user=new User();user.setOpenid("web:"+UUID.randomUUID());user.setNickname(input.getUsername());
        user.setGender(0);user.setStatus(1);users.save(user);
        bind(user.getId(),input);
        return issue(user,true);
    }
    @Transactional
    public void bind(Long id,PasswordLoginDTO input) {
        jdbc.update("INSERT INTO t_web_credential(user_id,username,password_hash) VALUES (?,?,?)",
                id,input.getUsername().toLowerCase(Locale.ROOT),PasswordHasher.hash(input.getPassword()));
    }
    public LoginVO issue(User user,boolean fresh) {
        String access=jwt.generateAccessToken(user.getId(),user.getOpenid());
        String refresh=jwt.generateRefreshToken(user.getId(),user.getOpenid());
        String key="runwise:token:"+user.getId();
        redis.execute(new DefaultRedisScript<>("redis.call('psetex',KEYS[1],ARGV[3],ARGV[1]); "
                + "redis.call('psetex',KEYS[2],ARGV[4],ARGV[2]); return 1",Long.class),
                Arrays.asList(key+":access",key+":refresh"),access,refresh,
                jwt.getAccessTokenExpire().toString(),jwt.getRefreshTokenExpire().toString());
        LoginVO result=new LoginVO();result.setAccessToken(access);result.setRefreshToken(refresh);
        result.setUserId(user.getId());result.setNickname(user.getNickname());result.setAvatarUrl(user.getAvatarUrl());
        result.setIsNewUser(fresh);result.setCreateTime(user.getCreateTime());return result;
    }
    public void logout(Long id) {redis.delete(Arrays.asList("runwise:token:"+id+":access","runwise:token:"+id+":refresh"));}
}

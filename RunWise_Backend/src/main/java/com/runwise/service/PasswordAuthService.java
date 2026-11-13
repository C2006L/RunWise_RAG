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
        User user=users.getById(id);
        if(user==null||!Integer.valueOf(1).equals(user.getStatus()))throw new BusinessException(ResultCode.UNAUTHORIZED);
        List<Map<String,Object>> own=jdbc.queryForList("SELECT username,password_hash FROM t_web_credential WHERE user_id=?",id);
        String name=input.getUsername().toLowerCase(Locale.ROOT);
        if(!own.isEmpty()) {
            if(name.equals(own.get(0).get("username"))&&PasswordHasher.matches(input.getPassword(),(String)own.get(0).get("password_hash")))return;
            throw new BusinessException(ResultCode.PARAM_ERROR,"当前账号已设置账密，请使用已有账号登录；此入口不修改密码");
        }
        if(jdbc.queryForObject("SELECT COUNT(*) FROM t_web_credential WHERE username=?",Integer.class,name)>0)
            throw new BusinessException(ResultCode.PARAM_ERROR,"用户名已被其他账号使用，不能自动合并数据");
        jdbc.update("INSERT INTO t_web_credential(user_id,username,password_hash) VALUES (?,?,?)",
                id,input.getUsername().toLowerCase(Locale.ROOT),PasswordHasher.hash(input.getPassword()));
    }
    public LoginVO issue(User user,boolean fresh) {return new RunnerSessions(jwt,redis).issue(user,fresh);}
    public void logout(Long id,String token) {new RunnerSessions(jwt,redis).logout(id,token);}
    public Map<String,Object> account(Long id) {
        User user=users.getById(id);if(user==null)throw new BusinessException(ResultCode.UNAUTHORIZED);
        List<String> names=jdbc.queryForList("SELECT username FROM t_web_credential WHERE user_id=?",String.class,id);
        Map<String,Object> result=new LinkedHashMap<>();result.put("userId",id);result.put("username",names.isEmpty()?null:names.get(0));
        result.put("wechatBound",user.getOpenid()!=null&&!user.getOpenid().startsWith("web:"));return result;
    }
}

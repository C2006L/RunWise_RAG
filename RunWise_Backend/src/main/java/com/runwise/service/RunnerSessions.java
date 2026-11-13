package com.runwise.service;
import com.runwise.common.*;
import com.runwise.entity.User;
import com.runwise.vo.LoginVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import io.jsonwebtoken.Claims;
import java.util.*;

/** Each login receives an independent server-issued session. No client-supplied user/device ID. */
public class RunnerSessions {
    private final JwtUtils jwt;private final StringRedisTemplate redis;
    public RunnerSessions(JwtUtils jwt,StringRedisTemplate redis){this.jwt=jwt;this.redis=redis;}
    public static String key(Long user,Claims claims){
        String sid=claims.get("sid",String.class);
        if(sid==null||!sid.matches("[a-f0-9-]{36}"))throw new BusinessException(ResultCode.UNAUTHORIZED,"登录凭证已升级，请重新登录");
        return "runwise:session:"+user+":"+sid;
    }
    public LoginVO issue(User user,boolean fresh){return pair(user,fresh,UUID.randomUUID().toString(),null);}
    public LoginVO rotate(User user,String old){
        Claims claims=jwt.parseToken(old);key(user.getId(),claims);
        return pair(user,false,claims.get("sid",String.class),old);
    }
    private LoginVO pair(User user,boolean fresh,String sid,String previous){
        String access=jwt.generateSessionToken(user.getId(),user.getOpenid(),sid,false);
        String refresh=jwt.generateSessionToken(user.getId(),user.getOpenid(),sid,true);
        String prefix="runwise:session:"+user.getId()+":"+sid;
        Long result;
        if(previous==null) result=redis.execute(new DefaultRedisScript<>("redis.call('psetex',KEYS[1],ARGV[3],ARGV[1]); redis.call('psetex',KEYS[2],ARGV[4],ARGV[2]); return 1",Long.class),
                Arrays.asList(prefix+":access",prefix+":refresh"),access,refresh,jwt.getAccessTokenExpire().toString(),jwt.getRefreshTokenExpire().toString());
        else result=redis.execute(new DefaultRedisScript<>("if redis.call('get',KEYS[2]) ~= ARGV[1] then return 0 end redis.call('psetex',KEYS[1],ARGV[4],ARGV[2]); redis.call('psetex',KEYS[2],ARGV[5],ARGV[3]); return 1",Long.class),
                Arrays.asList(prefix+":access",prefix+":refresh"),previous,access,refresh,jwt.getAccessTokenExpire().toString(),jwt.getRefreshTokenExpire().toString());
        if(!Long.valueOf(1).equals(result))throw new BusinessException(ResultCode.UNAUTHORIZED,"会话已失效，请重新登录");
        LoginVO vo=new LoginVO();vo.setAccessToken(access);vo.setRefreshToken(refresh);vo.setUserId(user.getId());vo.setNickname(user.getNickname());vo.setAvatarUrl(user.getAvatarUrl());vo.setCreateTime(user.getCreateTime());vo.setIsNewUser(fresh);return vo;
    }
    public void logout(Long user,String token){
        Claims claims=jwt.parseToken(token);String prefix=key(user,claims);
        redis.execute(new DefaultRedisScript<>("if redis.call('get',KEYS[1]) ~= ARGV[1] then return 0 end redis.call('del',KEYS[1],KEYS[2]); return 1",Long.class),Arrays.asList(prefix+":access",prefix+":refresh"),token);
    }
}

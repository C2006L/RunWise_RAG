package com.runwise.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.runwise.common.BusinessException;
import com.runwise.common.JwtUtils;
import com.runwise.common.ResultCode;
import com.runwise.dto.LoginDTO;
import com.runwise.entity.User;
import com.runwise.mapper.UserMapper;
import com.runwise.service.UserService;
import com.runwise.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Value("${runwise.wechat.appid}")
    private String appid;

    @Value("${runwise.wechat.secret}")
    private String secret;

    public UserServiceImpl(JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 用 code 换取 openid
        String openid = getOpenid(dto.getCode());
        if (openid == null) {
            throw new BusinessException(ResultCode.WECHAT_API_ERROR);
        }

        // 2. 查询或创建用户
        User user = getByOpenid(openid);
        boolean isNewUser = false;
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(dto.getNickname());
            user.setAvatarUrl(dto.getAvatarUrl());
            user.setGender(dto.getGender() != null ? dto.getGender() : 0);
            user.setStatus(1);
            save(user);
            isNewUser = true;
        } else {
            // 更新昵称头像（微信每次登录可能带回最新信息）
            if (dto.getNickname() != null) {
                user.setNickname(dto.getNickname());
            }
            if (dto.getAvatarUrl() != null) {
                user.setAvatarUrl(dto.getAvatarUrl());
            }
            updateById(user);
        }

        // 3. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 4. 生成 token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), openid);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), openid);

        // 5. 存入 Redis（用于黑名单校验和多端登录管理）
        String redisKey = "runwise:token:" + user.getId();
        redisTemplate.opsForValue().set(redisKey + ":access", accessToken,
                jwtUtils.getAccessTokenExpire(), TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(redisKey + ":refresh", refreshToken,
                jwtUtils.getRefreshTokenExpire(), TimeUnit.MILLISECONDS);

        // 6. 组装返回
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setIsNewUser(isNewUser);
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtils.isValid(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refresh_token已过期");
        }
        io.jsonwebtoken.Claims claims = jwtUtils.parseToken(refreshToken);
        if (!"refresh".equals(claims.get("type")) || claims.get("scope") != null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "需要有效的刷新凭证");
        Long userId = jwtUtils.getUserId(refreshToken);
        if(userId==null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus()))
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账户不可用");

        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getOpenid());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getOpenid());

        // 更新 Redis
        String redisKey = "runwise:token:" + user.getId();
        Long rotated = redisTemplate.execute(new DefaultRedisScript<>(
                "if redis.call('get',KEYS[2]) ~= ARGV[1] then return 0 end " +
                "redis.call('psetex',KEYS[1],ARGV[4],ARGV[2]); " +
                "redis.call('psetex',KEYS[2],ARGV[5],ARGV[3]); return 1", Long.class),
                Arrays.asList(redisKey + ":access",redisKey + ":refresh"),refreshToken,newAccessToken,newRefreshToken,
                jwtUtils.getAccessTokenExpire().toString(),jwtUtils.getRefreshTokenExpire().toString());
        if (!Long.valueOf(1L).equals(rotated))
            throw new BusinessException(ResultCode.UNAUTHORIZED,"刷新凭证已失效，请重新登录");

        LoginVO vo = new LoginVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setIsNewUser(false);
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public User getByOpenid(String openid) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
    }

    /**
     * 调用微信 code2session 接口换取 openid
     */
    private String getOpenid(String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code);
        try {
            String response = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(response);
            log.debug("微信登录响应已收到");
            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                log.error("微信登录失败: {}", response);
                return null;
            }
            return json.getStr("openid");
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            return null;
        }
    }
}

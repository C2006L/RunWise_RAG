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
        return vo;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtils.isValid(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refresh_token已过期");
        }
        Long userId = jwtUtils.getUserId(refreshToken);
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getOpenid());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getOpenid());

        // 更新 Redis
        String redisKey = "runwise:token:" + user.getId();
        redisTemplate.opsForValue().set(redisKey + ":access", newAccessToken,
                jwtUtils.getAccessTokenExpire(), TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(redisKey + ":refresh", newRefreshToken,
                jwtUtils.getRefreshTokenExpire(), TimeUnit.MILLISECONDS);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setIsNewUser(false);
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
            log.debug("微信登录返回: {}", response);
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

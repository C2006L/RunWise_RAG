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

        return new com.runwise.service.RunnerSessions(jwtUtils,redisTemplate).issue(user,isNewUser);
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

        return new com.runwise.service.RunnerSessions(jwtUtils,redisTemplate).rotate(user,refreshToken);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void bindWechat(Long userId,String code) {
        String openid=getOpenid(code);
        if(openid==null||openid.trim().isEmpty())throw new BusinessException(ResultCode.WECHAT_API_ERROR);
        User current=getById(userId);
        if(current==null||!Integer.valueOf(1).equals(current.getStatus()))throw new BusinessException(ResultCode.UNAUTHORIZED);
        if(openid.equals(current.getOpenid()))return;
        if(current.getOpenid()==null||!current.getOpenid().startsWith("web:"))
            throw new BusinessException(ResultCode.PARAM_ERROR,"当前账号已绑定其他微信，不能直接替换");
        User existing=getByOpenid(openid);
        if(existing!=null)throw new BusinessException(ResultCode.PARAM_ERROR,"此微信已有独立账号，请登录原微信账号绑定账密；暂不合并两份数据");
        boolean changed=update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId,userId).eq(User::getOpenid,current.getOpenid()).set(User::getOpenid,openid));
        if(!changed)throw new BusinessException(ResultCode.PARAM_ERROR,"绑定状态已变化，请刷新后重试");
    }

    @Override
    public User getByOpenid(String openid) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
    }

    /**
     * 调用微信 code2session 接口换取 openid
     */
    protected String getOpenid(String code) {
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

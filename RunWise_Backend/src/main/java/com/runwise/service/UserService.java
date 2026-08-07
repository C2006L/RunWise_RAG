package com.runwise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.runwise.dto.LoginDTO;
import com.runwise.entity.User;
import com.runwise.vo.LoginVO;

public interface UserService extends IService<User> {

    /**
     * 微信登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 刷新 token
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 根据 openid 查询用户
     */
    User getByOpenid(String openid);
}

package com.runwise.vo;

import lombok.Data;

/**
 * 登录返回结果
 */
@Data
public class LoginVO {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isNewUser;
}

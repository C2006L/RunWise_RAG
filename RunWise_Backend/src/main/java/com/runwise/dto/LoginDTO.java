package com.runwise.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信登录请求参数
 */
@Data
public class LoginDTO {

    @NotBlank(message = "code不能为空")
    private String code;

    private String nickname;

    private String avatarUrl;

    private Integer gender;
}

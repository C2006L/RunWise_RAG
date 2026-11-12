package com.runwise.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class PasswordLoginDTO {
    @NotNull @Pattern(regexp="[A-Za-z0-9_.-]{3,32}",message="用户名需为3-32位字母数字或_.-")
    private String username;
    @NotBlank @Size(min=12,max=128,message="密码长度需为12-128位") private String password;
}

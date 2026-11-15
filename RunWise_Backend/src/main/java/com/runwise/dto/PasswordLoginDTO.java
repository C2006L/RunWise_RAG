package com.runwise.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class PasswordLoginDTO {
    @NotNull @Pattern(regexp="[\u3400-\u9fffA-Za-z0-9_.-]{2,32}",message="账号名需为2-32位中文、英文字母、数字或_.-")
    private String username;
    @NotBlank @Size(min=12,max=128,message="密码长度需为12-128位") private String password;
}

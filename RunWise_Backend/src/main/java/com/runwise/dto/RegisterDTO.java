package com.runwise.dto;
import javax.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
public class RegisterDTO extends PasswordLoginDTO {
    @NotBlank(message="请再次输入密码") @Size(max=128) private String confirmPassword;
    @AssertTrue(message="两次输入的密码不一致")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPasswordsMatching(){return getPassword()!=null&&getPassword().equals(confirmPassword);}
}

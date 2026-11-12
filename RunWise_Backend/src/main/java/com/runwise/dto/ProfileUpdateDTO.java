package com.runwise.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProfileUpdateDTO {
    @Size(max=64) private String nickname;
    @Size(max=512) @Pattern(regexp="^(https?://.*|/uploads/.*)?$", message="头像地址格式不正确") private String avatarUrl;
    @Min(0) @Max(2) private Integer gender;
    @PastOrPresent private LocalDate birthday;
    @DecimalMin("50") @DecimalMax("250") private BigDecimal height;
    @DecimalMin("20") @DecimalMax("400") private BigDecimal weight;
    @Min(0) @Max(10) private Integer runningGoal;
    @Size(max=32) private String runningLevel;
}
